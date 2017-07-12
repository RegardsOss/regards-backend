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

import java.util.List;

import org.springframework.mail.SimpleMailMessage;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Strategy interface to handle CRUD operations on EmailDTO entities and mailing tasks
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IEmailService {

    /**
     * Retrieves the list of emails
     *
     * @return A {@code List} of {@code Email}s
     */
    List<Email> retrieveEmails();

    /**
     * Sends the passed email to the passed recipients and save a representation in DB.
     *
     * @param pEmail
     *            The ready-to-send email. Must not be <code>null</code>.
     * @return The sent email
     */
    SimpleMailMessage sendEmail(SimpleMailMessage pEmail);

    /**
     * Retrieves the email of passed id
     *
     * @param pId
     *            The email id
     * @return The email as {@link Email}
     */
    Email retrieveEmail(Long pId) throws ModuleException;

    /**
     * Re-sends the email of passed id
     *
     * @param pId
     *            The email id
     * @return
     */
    void resendEmail(Long pId) throws ModuleException;

    /**
     * Deletes the email of passed id
     *
     * @param pId
     *            The email id
     * @return
     */
    void deleteEmail(Long pId);

    /**
     * Checks if an email of passed id exists
     *
     * @param pId
     *            The email id
     * @return <code>true</code> if exists, else <code>false</code>
     */
    boolean exists(Long pId);

}
