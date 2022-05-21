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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.listeners;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.AccessRightTemplateConf;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.events.OnRefuseAccountEvent;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
     * @param pEvent the init event
     */
    private void sendAccountRefusedEmail(final OnRefuseAccountEvent pEvent) {
        // Retrieve the account
        final Account account = pEvent.getAccount();

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", account.getFirstName());

        String message;
        try {
            message = templateService.render(AccessRightTemplateConf.ACCOUNT_REFUSED_TEMPLATE_NAME, data);
        } catch (final TemplateException e) {
            LOG.error(
                "Could not find the template to generate the email notifying the account refusal. Falling back to default.",
                e);
            message = "Your access request was refused by admin.";
        }

        // Send it
        try {
            FeignSecurityManager.asSystem();
            emailClient.sendEmail(message, "[REGARDS] Account refused", null, account.getEmail());
        } finally {
            FeignSecurityManager.reset();
        }
    }

}