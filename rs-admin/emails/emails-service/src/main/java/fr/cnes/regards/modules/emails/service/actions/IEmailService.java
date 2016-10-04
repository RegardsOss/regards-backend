/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service.actions;

import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.domain.Recipient;

/**
 * Strategy interface to handle CRUD operations on Email entities and mailing tasks
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IEmailService {

    /**
     * Retrieves the list of emails
     *
     * @return
     */
    Iterable<Email> retrieveEmails();

    /**
     * Sends the passed email to the passed recipients
     *
     * @param pRecipients
     *            The list of recipients
     * @param pEmail
     *            The ready-to-send email
     * @return
     */
    Email sendEmail(Iterable<Recipient> pRecipients, Email pEmail);

    /**
     * Retrieves the email of passed id
     *
     * @param pId
     *            The email id
     * @return
     */
    Email retrieveEmail(Long pId);

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

}
