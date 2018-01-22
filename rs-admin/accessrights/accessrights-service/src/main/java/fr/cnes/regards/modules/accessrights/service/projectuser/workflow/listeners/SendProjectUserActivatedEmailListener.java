/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.mail.SimpleMailMessage;
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
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * Listen to {@link OnActiveEvent} in order to warn the user its account request was re-activated.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class SendProjectUserActivatedEmailListener implements ApplicationListener<OnActiveEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SendProjectUserActivatedEmailListener.class);

    /**
     * The email template
     */
    private static final String TEMPLATE = TemplateServiceConfiguration.PROJECT_USER_ACTIVATED_TEMPLATE_CODE;

    /**
     * Service handling CRUD operations on email templates
     */
    private final ITemplateService templateService;

    /**
     * Client for sending emails
     */
    private final IEmailClient emailClient;

    /**
     * Account service
     */
    private final IAccountsClient accountsClient;

    /**
     * @param pTemplateService
     * @param pEmailClient
     * @param accountsClient
     */
    public SendProjectUserActivatedEmailListener(ITemplateService pTemplateService, IEmailClient pEmailClient,
            IAccountsClient accountsClient) {
        super();
        templateService = pTemplateService;
        emailClient = pEmailClient;
        this.accountsClient = accountsClient;
    }

    /**
     * Send a password reset email based on information stored in the passed event
     *
     * @param pEvent
     *            the init event
     */
    @Override
    public void onApplicationEvent(final OnActiveEvent pEvent) {
        // Retrieve the user
        ProjectUser projectUser = pEvent.getProjectUser();

        // Build the list of recipients
        String[] recipients = { projectUser.getEmail() };

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

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOGGER.error(
                    "Could not find the template to generate the email notifying the account refusal. Falling back to default.",
                    e);
            email = writeToEmailDefault(data, recipients);
        }

        // Send it
        FeignSecurityManager.asSystem();
        emailClient.sendEmail(email);
        FeignSecurityManager.reset();
    }

    /**
     * Send super simple mail in case the template service fails
     *
     * @param data the data
     * @param recipients the recipients
     * @return the result email
     */
    private SimpleMailMessage writeToEmailDefault(Map<String, String> data, String[] recipients) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipients);
        email.setSubject("REGARDS - Access re-activated");
        email.setText("Your access has been re-activated.");
        return email;
    }

}