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
package fr.cnes.regards.modules.accessrights.instance.service.passwordreset;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Listen to {@link OnPasswordResetEvent} in order to send a password reset to the user when required.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class PasswordResetListener implements ApplicationListener<OnPasswordResetEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetListener.class);

    /**
     * The password reset email template code
     */
    private static final String MDP_RESET_TEMPLATE = "passwordResetTemplate";

    /**
     * The password reset service. Autowired by Spring.
     */
    private final IPasswordResetService passwordResetService;

    /**
     * Template Service. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param pPasswordResetService
     *            the password reset service
     * @param pTemplateService
     *            the template service
     * @param pEmailClient
     *            the email client
     */
    public PasswordResetListener(final IPasswordResetService pPasswordResetService,
            final ITemplateService pTemplateService, final IEmailClient pEmailClient) {
        super();
        passwordResetService = pPasswordResetService;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    @Override
    public void onApplicationEvent(final OnPasswordResetEvent pEvent) {
        sendPasswordResetEmail(pEvent);
    }

    /**
     * Send a password reset email based on information stored in the passed event
     *
     * @param pEvent
     *            the init event
     */
    private void sendPasswordResetEmail(final OnPasswordResetEvent pEvent) {
        // Retrieve the account
        final Account account = pEvent.getAccount();

        // Create the token
        final String token = UUID.randomUUID().toString();
        passwordResetService.createPasswordResetToken(account, token);

        // Build the list of recipients
        final String[] recipients = { account.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", account.getFirstName());
        data.put("requestLink", pEvent.getRequestLink());
        data.put("originUrl", pEvent.getOriginUrl());
        data.put("token", token);
        data.put("accountEmail", account.getEmail());

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(MDP_RESET_TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template to generate a password reset email. Falling back to default.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Password Reset");

            String linkUrlTemplate;
            if ((pEvent.getRequestLink() != null) && pEvent.getRequestLink().contains("?")) {
                linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
            } else {
                linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
            }
            final String linkUrl = String
                    .format(linkUrlTemplate, pEvent.getRequestLink(), pEvent.getOriginUrl(), token, account.getEmail());
            email.setText("Please click on the following link to set a new password for your account: " + linkUrl);
        }

        // Send it
        FeignSecurityManager.asSystem();
        emailClient.sendEmail(email);
        FeignSecurityManager.reset();
    }
}