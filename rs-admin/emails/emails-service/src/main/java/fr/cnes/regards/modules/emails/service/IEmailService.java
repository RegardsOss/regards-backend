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

import java.util.List;

import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Strategy interface to handle CRUD operations on EmailDTO entities and mailing tasks
 * @author Xavier-Alexandre Brochard
 */
public interface IEmailService {

    /**
     * Retrieve the list of emails
     * @return A {@code List} of {@code Email}s
     */
    List<Email> retrieveEmails();

    /**
     * Helper method to send mail without creating SimpleMailMessage before call
     * @param message email message
     * @param subject email subject
     * @param from email sender, if you don't care about who is sending, set null to use default.
     * @param to recipients
     */
    default Email sendEmail(String message, String subject, String from, String... to) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setSubject(subject);
        mail.setFrom(from);
        mail.setText(message);
        mail.setTo(to);
        return sendEmail(mail);
    }

    /**
     * Send given email to given recipients and save a representation in DB.
     * @param pEmail The ready-to-send email. Must not be <code>null</code>.
     * @return The sent email
     */
    Email sendEmail(SimpleMailMessage pEmail);

    /**
     * Send given email with given attachment and save a representation in DataBase.
     * <b>Attachment is zipped</b>
     */
    Email sendEmail(SimpleMailMessage email, String attName, InputStreamSource attSource);

    /**
     * Retrieve email
     * @param id The email id
     * @return The email as {@link Email}
     */
    Email retrieveEmail(Long id) throws ModuleException;

    /**
     * Re-send email
     * @param id The email id
     */
    void resendEmail(Long id) throws ModuleException;

    /**
     * Delete email
     * @param id The email id
     */
    void deleteEmail(Long id);

    /**
     * Check if an email exist
     * @param id The email id
     * @return <code>true</code> if exists, else <code>false</code>
     */
    boolean exists(Long id);

}
