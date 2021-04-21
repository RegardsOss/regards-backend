/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * Implementation of the {@link ISendingStrategy}.<br>
 * Sends the notifications as emails.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class EmailSendingStrategy implements ISendingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendingStrategy.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.sender.no.reply:regards@noreply.fr}")
    private String defaultSender;

    @Override
    public void send(final Notification notification, final String[] recipients) {
        // Build the email from the notification and add recipients
        MimeMessage mimeMsg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg);
            helper.setText(notification.getMessage(), notification.getMimeType().includes(MimeTypeUtils.TEXT_HTML));
            helper.setTo(recipients);
            helper.setFrom(defaultSender);
            helper.setSentDate(new Date());
            helper.setSubject("[" + notification.getSender() + "]" + notification.getTitle());
            // Send the mail
            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
