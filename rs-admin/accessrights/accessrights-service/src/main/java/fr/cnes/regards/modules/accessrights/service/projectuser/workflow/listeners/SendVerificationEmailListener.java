/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;

/**
 * Listen to {@link OnGrantAccessEvent} in order to warn the user its account request was refused.
 * @author Xavier-Alexandre Brochard
 */
@Profile("!nomail")
@Component
public class SendVerificationEmailListener implements ApplicationListener<OnGrantAccessEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendVerificationEmailListener.class);

    private final ITemplateService templateService;

    private final IEmailService emailService;

    private final IAccountsClient accountsClient;

    /**
     * Service to manage email verification tokens for project users.
     */
    private final IEmailVerificationTokenService emailVerificationTokenService;

    public SendVerificationEmailListener(ITemplateService templateService, IEmailService emailService,
            IAccountsClient accountsClient, IEmailVerificationTokenService emailVerificationTokenService) {
        super();
        this.templateService = templateService;
        this.emailService = emailService;
        this.accountsClient = accountsClient;
        this.emailVerificationTokenService = emailVerificationTokenService;
    }

    @Override
    public void onApplicationEvent(final OnGrantAccessEvent event) {
        // Retrieve the project user
        ProjectUser projectUser = event.getProjectUser();

        // Retrieve the address
        String userEmail = projectUser.getEmail();

        // Retrieve the token
        EmailVerificationToken token;
        try {
            token = emailVerificationTokenService.findByProjectUser(projectUser);
        } catch (EntityNotFoundException e) {
            LOGGER.error("Could not retrieve the verification token. Aborting the email sending.", e);
            return;
        }

        // Create a hash map in order to store the data to inject in the mail
        Map<String, String> data = new HashMap<>();
        // lets retrive the account
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<Resource<Account>> accountResponse = accountsClient
                    .retrieveAccounByEmail(projectUser.getEmail());
            if (accountResponse.getStatusCode().is2xxSuccessful()) {
                data.put("name", accountResponse.getBody().getContent().getFirstName());
            } else {
                LOGGER.error("Could not find the associated Account for templating the email content.");
                data.put("name", "");
            }
        } catch (FeignException e) {
            LOGGER.error("Could not find the associated Account for templating the email content.", e);
            data.put("name", "");
        } finally {
            FeignSecurityManager.reset();
        }

        String linkUrlTemplate;
        if (token.getRequestLink().contains("?")) {
            linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
        } else {
            linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
        }
        String confirmationUrl = String.format(linkUrlTemplate, token.getRequestLink(),
                                               UriUtils.encode(token.getOriginUrl(), StandardCharsets.UTF_8.name()),
                                               token.getToken(), userEmail);
        data.put("confirmationUrl", confirmationUrl);

        String message;
        try {
            message = templateService.render(AccessRightTemplateConf.EMAIL_ACCOUNT_VALIDATION_TEMPLATE_NAME, data);
        } catch (TemplateException e) {
            LOGGER.warn("Could not find the template for registration confirmation. Falling back to default template.",
                        e);
            message = "Please click on the following link to confirm your registration: " + data.get("confirmationUrl");
        }
        emailService.sendEmail(message, "[REGARDS] Account Confirmation", null, userEmail);
    }
}