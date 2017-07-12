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
package fr.cnes.regards.modules.emails.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 *
 * Class AbstractEmailService
 *
 * Standard function of mail service to handle sending mails.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractEmailService implements IEmailService {

    /**
     *
     * Mail sender to use.
     *
     * @return {@link JavaMailSender}
     * @since 1.0-SNASHOT
     */
    protected abstract JavaMailSender getMailSender();

    /**
     *
     * Class logger
     *
     * @return {@link Logger}
     * @since 1.0-SNAPSHOT
     */
    protected abstract Logger getLogger();

    /**
     *
     * Send the given mail thanks to the java mail sender get by the static getMailSender method
     *
     * @param pMessage
     *            {@link SimpleMailMessage} mail to send
     * @return {@link SimpleMailMessage} sent
     * @since 1.0-SNAPSHOT
     */
    public SimpleMailMessage sendMailWithSender(final SimpleMailMessage pMessage) {

        final MimeMessage message = getMailSender().createMimeMessage();
        try {
            final MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setText(pMessage.getText(), true);
            helper.setTo(pMessage.getTo());
            if (pMessage.getBcc() != null) {
                helper.setBcc(pMessage.getBcc());
            }
            if (pMessage.getCc() != null) {
                helper.setCc(pMessage.getCc());
            }
            if (pMessage.getFrom() != null) {
                helper.setFrom(pMessage.getFrom());
            }
            if (pMessage.getReplyTo() != null) {
                helper.setReplyTo(pMessage.getReplyTo());
            }
            if (pMessage.getSentDate() != null) {
                helper.setSentDate(pMessage.getSentDate());
            }
            if (pMessage.getSubject() != null) {
                helper.setSubject(pMessage.getSubject());
            }
            // Send the mail
            getMailSender().send(message);
        } catch (final MessagingException e) {
            getLogger().error("Error sending mail", e);
        }

        return pMessage;

    }

}
