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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.listeners;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.events.OnRefuseAccountEvent;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Listen to {@link OnRefuseAccountEvent} in order to warn the user its account request was refused.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class SendAccountRefusedEmailListener implements ApplicationListener<OnRefuseAccountEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SendAccountRefusedEmailListener.class);

    /**
     * The email template for refused account
     */
    private static final String ACCOUNT_REFUSED_TEMPLATE = "accountRefusedTemplate";

    /**
     * Service handling CRUD operations on email templates
     */
    private final ITemplateService templateService;

    /**
     * Client for sending emails
     */
    private final IEmailClient emailClient;

    /**
     * @param pTemplateService
     * @param pEmailClient
     */
    public SendAccountRefusedEmailListener(ITemplateService pTemplateService, IEmailClient pEmailClient) {
        super();
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    @Override
    public void onApplicationEvent(final OnRefuseAccountEvent pEvent) {
        sendAccountRefusedEmail(pEvent);
    }

    /**
     * Send a password reset email based on information stored in the passed event
     *
     * @param pEvent
     *            the init event
     */
    private void sendAccountRefusedEmail(final OnRefuseAccountEvent pEvent) {
        // Retrieve the account
        final Account account = pEvent.getAccount();

        // Build the list of recipients
        final String[] recipients = { account.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", account.getFirstName());

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(ACCOUNT_REFUSED_TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.error("Could not find the template to generate the email notifying the account refusal. Falling back to default.",
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
     * @param data the data
     * @param recipients the recipients
     * @return the result email
     */
    private SimpleMailMessage writeToEmailDefault(Map<String, String> data, String[] recipients) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipients);
        email.setSubject("REGARDS - Access refused");
        email.setText("Your access request was refused by admin.");
        return email;
    }

}