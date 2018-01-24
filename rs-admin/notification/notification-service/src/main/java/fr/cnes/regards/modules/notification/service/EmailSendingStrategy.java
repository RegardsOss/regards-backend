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
package fr.cnes.regards.modules.notification.service;

import java.util.Date;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * Implementation of the {@link INotificationSendingStrategy}.<br>
 * Sends the notifications as emails.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class EmailSendingStrategy implements ISendingStrategy {

    /**
     * Feign client from module Email
     */
    private final IEmailClient emailClient;

    /**
     * Creates new strategy with passed email client
     *
     * @param pEmailClient
     *            The email feign client
     */
    public EmailSendingStrategy(final IEmailClient pEmailClient) {
        emailClient = pEmailClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.ISendingStrategy#send(fr.cnes.regards.modules.
     * notification.domain.Notification)
     */
    @Override
    public void send(final Notification pNotification, final String[] pRecipients) {
        // Build the email from the notification and add recipients
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom("regards@noreply.com");
        email.setSentDate(new Date());
        email.setSubject("[" + pNotification.getSender() + "]" + pNotification.getTitle());
        email.setText(pNotification.getMessage());
        email.setTo(pRecipients);

        // Send the email
        emailClient.sendEmail(email);
    }

}
