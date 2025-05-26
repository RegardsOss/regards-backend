/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.config.AccessRightsTemplateConfiguration;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailService;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailWrapper;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Listen to {@link OnGrantAccessEvent} to let the user know their access was granted.
 *
 * @author Xavier-Alexandre Brochard
 */
@Profile("!nomail")
@Component
public class SendProjectUserGrantedEmailListener implements ApplicationListener<OnGrantAccessEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendProjectUserGrantedEmailListener.class);

    @Value("${regards.mails.noreply.address:regards@noreply.fr}")
    private String noreply;

    private final QuotaHelperService quotaHelperService;

    private final AccessRightsEmailService accessRightsEmailService;

    public SendProjectUserGrantedEmailListener(QuotaHelperService quotaHelperService,
                                               AccessRightsEmailService accessRightsEmailService) {
        this.quotaHelperService = quotaHelperService;
        this.accessRightsEmailService = accessRightsEmailService;
    }

    @Override
    public void onApplicationEvent(final OnGrantAccessEvent event) {

        ProjectUser projectUser = event.getProjectUser();
        String userEmail = projectUser.getEmail();

        Map<String, Object> data = new HashMap<>();

        // quota management: unlimited / not interesting while storage does not answer
        DownloadQuotaLimitsDto quotaLimits = quotaHelperService.getQuota(userEmail);
        if (quotaLimits != null) {
            data.put("quota", Optional.ofNullable(quotaLimits.getMaxQuota()).orElse(-1L));
            data.put("rate", Optional.ofNullable(quotaLimits.getRateLimit()).orElse(-1L));
        } else {
            LOGGER.error("Could not find the associated quota limits for templating the email content.");
            data.put("quota", -1L);
            data.put("rate", -1L);
        }
        // For compatibility with old custom templates which used to include a confirmation URL (the template
        // engine fails if a template uses a parameter that is not provided)
        data.put("confirmationUrl", "");

        AccessRightsEmailWrapper wrapper = new AccessRightsEmailWrapper().setProjectUser(projectUser)
                                                                         .setSubject("[REGARDS] Project Registration")
                                                                         .setFrom(noreply)
                                                                         .setTo(Collections.singleton(userEmail))
                                                                         .setTemplate(AccessRightsTemplateConfiguration.EMAIL_ACCOUNT_VALIDATION_TEMPLATE_NAME)
                                                                         .setData(data)
                                                                         .setDefaultMessage(
                                                                             "Your project registration is confirmed.");

        accessRightsEmailService.sendEmail(wrapper);
    }
}