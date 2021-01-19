/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Class AbstractEmailService
 *
 * Standard function of mail service to handle sending mails.
 * @author Sébastien Binda

 */
public abstract class AbstractEmailService implements IEmailService {

    /**
     * Mail sender to use.
     * @return {@link JavaMailSender}
     * @since 1.0-SNASHOT
     */
    protected abstract JavaMailSender getMailSender();

    /**
     * Class logger
     * @return {@link Logger}

     */
    protected abstract Logger getLogger();

    /**
     * Send the given mail thanks to the java mail sender get by the static getMailSender method
     * @param message {@link SimpleMailMessage} mail to send
     * @return {@link SimpleMailMessage} sent

     */
    public SimpleMailMessage sendMailWithSender(final SimpleMailMessage message, String defaultSender) {
        return sendMailWithSender(message, null, null, defaultSender);
    }

    /**
     * Send the given mail thanks to the java mail sender get by the static getMailSender method
     * @param message {@link SimpleMailMessage} mail to send
     * @return {@link SimpleMailMessage} sent

     */
    public SimpleMailMessage sendMailWithSender(final SimpleMailMessage message, String attachmentName,
            InputStreamSource attSource, String defaultSender) {
        final MimeMessage mimeMsg = getMailSender().createMimeMessage();
        try {
            boolean withAttachment = (attachmentName != null) && (attSource != null);
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, withAttachment);
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
                helper.setFrom(defaultSender);
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
            // Send the mail
            getMailSender().send(mimeMsg);
        } catch (final MessagingException e) {
            getLogger().error("Error sending mail", e);
        }

        return message;
    }

}
