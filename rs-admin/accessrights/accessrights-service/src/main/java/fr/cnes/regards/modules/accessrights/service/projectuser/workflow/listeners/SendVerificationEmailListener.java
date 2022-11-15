/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.config.AccessRightsTemplateConfiguration;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailService;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailWrapper;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Listen to {@link OnGrantAccessEvent} in order to warn the user its account request was refused.
 *
 * @author Xavier-Alexandre Brochard
 */
@Profile("!nomail")
@Component
public class SendVerificationEmailListener implements ApplicationListener<OnGrantAccessEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendVerificationEmailListener.class);

    @Value("${regards.mails.noreply.address:regards@noreply.fr}")
    private String noreply;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    private final IStorageRestClient storageClient;

    private final AccessRightsEmailService accessRightsEmailService;

    public SendVerificationEmailListener(IStorageRestClient storageClient,
                                         IEmailVerificationTokenService emailVerificationTokenService,
                                         AccessRightsEmailService accessRightsEmailService) {
        this.storageClient = storageClient;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.accessRightsEmailService = accessRightsEmailService;
    }

    @Override
    public void onApplicationEvent(final OnGrantAccessEvent event) {

        ProjectUser projectUser = event.getProjectUser();
        String userEmail = projectUser.getEmail();

        EmailVerificationToken token;
        try {
            token = emailVerificationTokenService.findByProjectUser(projectUser);
            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                emailVerificationTokenService.renewToken(projectUser);
            }
        } catch (EntityNotFoundException e) {
            LOGGER.error(String.format(
                "Could not retrieve the verification token for the user %s. Aborting the email sending.",
                projectUser.getEmail()), e);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        String linkUrlTemplate;
        if (token.getRequestLink().contains("?")) {
            linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
        } else {
            linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
        }
        String confirmationUrl = String.format(linkUrlTemplate,
                                               token.getRequestLink(),
                                               UriUtils.encode(token.getOriginUrl(), StandardCharsets.UTF_8.name()),
                                               token.getToken(),
                                               userEmail);
        data.put("confirmationUrl", confirmationUrl);

        // quota management: unlimited / not interesting while storage does not answer
        data.put("quota", -1L);
        data.put("rate", -1L);
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<DownloadQuotaLimitsDto> storageResponse = storageClient.getQuotaLimits(userEmail);
            if (storageResponse.getStatusCode().is2xxSuccessful()) {
                DownloadQuotaLimitsDto quotaLimits = ResponseEntityUtils.extractBodyOrThrow(storageResponse,
                                                                                            "Cannot get quota limit of "
                                                                                            + userEmail);
                data.put("quota", Optional.ofNullable(quotaLimits.getMaxQuota()).orElse(-1L));
                data.put("rate", Optional.ofNullable(quotaLimits.getRateLimit()).orElse(-1L));
            } else {
                LOGGER.error("Could not find the associated quota limits for templating the email content.");
            }
        } catch (HttpServerErrorException | HttpClientErrorException | ModuleException e) {
            LOGGER.debug("Could not add quota paragraph to the email content.", e);
        } finally {
            FeignSecurityManager.reset();
        }

        AccessRightsEmailWrapper wrapper = new AccessRightsEmailWrapper().setProjectUser(projectUser)
                                                                         .setSubject("[REGARDS] Account Confirmation")
                                                                         .setFrom(noreply)
                                                                         .setTo(Collections.singleton(userEmail))
                                                                         .setTemplate(AccessRightsTemplateConfiguration.EMAIL_ACCOUNT_VALIDATION_TEMPLATE_NAME)
                                                                         .setData(data)
                                                                         .setDefaultMessage(
                                                                             "Please click on the following link to confirm your registration: "
                                                                             + data.get("confirmationUrl"));

        accessRightsEmailService.sendEmail(wrapper);
    }
}