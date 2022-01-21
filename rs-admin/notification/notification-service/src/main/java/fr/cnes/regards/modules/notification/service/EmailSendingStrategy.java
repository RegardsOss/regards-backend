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
package fr.cnes.regards.modules.notification.service;

import java.util.Date;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * Implementation of the {@link ISendingStrategy}.<br>
 * Sends the notifications as emails.
 *
 * @author Xavier-Alexandre Brochard
 */
@Profile("!nomail")
@Component
public class EmailSendingStrategy implements ISendingStrategy {

    private static final int MAX_MAIL_LENGTH = 5120;

    /**
     * Feign client from module Email
     */
    private final IEmailService emailService;

    /**
     * Creates new strategy with passed email client
     *
     * @param emailService
     *            The email feign client
     */
    public EmailSendingStrategy(final IEmailService emailService) {
        this.emailService = emailService;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.ISendingStrategy#send(fr.cnes.regards.modules.
     * notification.domain.Notification)
     */
    @Override
    public void send(final Notification notification, final String[] recipients) {
        // Build the email from the notification and add recipients
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSentDate(new Date());
        email.setSubject("[" + notification.getSender() + "]" + notification.getTitle());
        email.setText(notification.getMessage());
        email.setTo(recipients);

        String message = notification.getMessage();
        if (message.length() > MAX_MAIL_LENGTH) {
            message = message.substring(0, MAX_MAIL_LENGTH) + " ... [Too long message was truncated]";
        }

        // Send the email
        emailService.sendEmail(email);
    }

}
