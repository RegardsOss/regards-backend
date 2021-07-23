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
package fr.cnes.regards.modules.emails.service;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Class AbstractEmailService
 *
 * Standard function of mail service to handle sending mails.
 * @author Sébastien Binda
 */
public abstract class AbstractEmailService implements IEmailService {

    protected abstract JavaMailSender getMailSender();

    protected abstract Logger getLogger();

    public void sendMailWithSender(SimpleMailMessage message, String sender) {
        sendMailWithSender(message, null, null, sender);
    }

    public void sendMailWithSender(SimpleMailMessage message, String attachmentName, InputStreamSource attSource, String sender) {

        try {

            MimeMessage mimeMsg = getMailSender().createMimeMessage();
            boolean withAttachment = (attachmentName != null) && (attSource != null);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, withAttachment);

            helper.setText(message.getText(), true);
            helper.setTo(message.getTo());
            if (message.getBcc() != null) {
                helper.setBcc(message.getBcc());
            }
            if (message.getCc() != null) {
                helper.setCc(message.getCc());
            }
            if ((message.getFrom() != null) && !message.getFrom().isEmpty()) {
                helper.setFrom(message.getFrom());
            } else {
                helper.setFrom(sender);
            }
            if (message.getReplyTo() != null) {
                helper.setReplyTo(message.getReplyTo());
            }
            if (message.getSentDate() != null) {
                helper.setSentDate(message.getSentDate());
            }
            if (message.getSubject() != null) {
                helper.setSubject(message.getSubject());
            }
            if (withAttachment) {
                helper.addAttachment(attachmentName, attSource);
            }

            getMailSender().send(mimeMsg);

        } catch (MessagingException | MailException e) {
            getLogger().warn("Unable to send mail. Recipient: {} - Subject: {} - Root Cause: {}",
                             message.getTo(),
                             message.getSubject(),
                             Throwables.getRootCause(e).toString());
        }
    }

}
