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
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link ISendingStrategy}.<br>
 * Sends the notifications as emails.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Profile("!nomail")
@Component
public class EmailSendingStrategy implements ISendingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendingStrategy.class);

    /**
     * Feign client from module emails
     */
    // Use the feign client in order to avoid a cyclic reference between two instance modules
    // (notification-instance-service and emails-instance-service)
    @Autowired
    private IEmailClient emailClient;

    @Override
    public void send(final Notification notification, final String[] recipients) {
        // Send the email
        String subject = "[" + notification.getSender() + "]" + notification.getTitle();
        try {
            FeignSecurityManager.asInstance();
            emailClient.sendEmail(notification.getMessage(), subject, null, recipients);
            LOGGER.info("Send mail after notification. Recipient: [{}] - Subject: [{}]", recipients, subject);
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
