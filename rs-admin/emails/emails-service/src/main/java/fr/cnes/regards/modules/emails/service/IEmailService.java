/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Strategy interface to handle CRUD operations on EmailDTO entities and mailing tasks
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IEmailService {

    /**
     * Retrieves the list of emails
     *
     * @return A {@link List} of emails as {@link MimeMessage}
     */
    List<MimeMessage> retrieveEmails();

    /**
     * Sends the passed email to the passed recipients
     *
     * @param pRecipients
     *            The list of recipients
     * @param pEmail
     *            The ready-to-send email
     * @return The sent email as {@link MimeMessage}
     * @throws MessagingException
     */
    MimeMessage sendEmail(String[] pRecipients, MimeMessage pEmail) throws MessagingException;

    /**
     * Retrieves the email of passed id
     *
     * @param pId
     *            The email id
     * @return The email as {@link MimeMessage}
     */
    MimeMessage retrieveEmail(Long pId);

    /**
     * Re-sends the email of passed id
     *
     * @param pId
     *            The email id
     * @return
     */
    void resendEmail(Long pId);

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
