/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnActiveEvent;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Listen to {@link OnActiveEvent} in order to warn the user its account request was re-activated.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class SendProjectUserActivatedEmailListener implements ApplicationListener<OnActiveEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendProjectUserActivatedEmailListener.class);

    private final ITemplateService templateService;

    private final IEmailClient emailClient;

    private final IAccountsClient accountsClient;

    public SendProjectUserActivatedEmailListener(ITemplateService pTemplateService, IEmailClient pEmailClient,
            IAccountsClient accountsClient) {
        templateService = pTemplateService;
        emailClient = pEmailClient;
        this.accountsClient = accountsClient;
    }

    @Override
    public void onApplicationEvent(final OnActiveEvent event) {
        // Retrieve the user
        ProjectUser projectUser = event.getProjectUser();

        // Create a hash map in order to store the data to inject in the mail
        Map<String, String> data = new HashMap<>();
        // lets retrive the account
        try {
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
        }

        String message;
        try {
            message = templateService.render(AccessRightTemplateConf.USER_ACTIVATED_TEMPLATE_NAME, data);
        } catch (final EntityNotFoundException e) {
            LOGGER.error(
                    "Could not find the template to generate the email notifying the account refusal. Falling back to default.",
                    e);
            message = "Your access has been enabled.";
        }

        // Send it
        try {
            FeignSecurityManager.asSystem();
            emailClient.sendEmail(message, "[REGARDS] User access enabled", null, projectUser.getEmail());
        } finally {
            FeignSecurityManager.reset();
        }
    }
}