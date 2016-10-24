/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import java.util.Set;

import javax.validation.Valid;

/**
 * Data Transfer Object wrapping the {@link Email} and a list of recipients received.The point is to match the
 * two-parameters "sendEmail" endpoint requirement.
 *
 * @author CS SI
 *
 */
public class EmailWithRecipientsDTO {

    /**
     * Set of recipients' email addresses
     */
    @Valid
    private Set<Recipient> recipients;

    /**
     * The mail
     */
    private Email email;

    /**
     * Get recipients
     *
     * @return the set of recipients' email addresses
     */
    public Set<Recipient> getRecipients() {
        return recipients;
    }

    /**
     * Set recipients
     *
     * @param pRecipients
     *            The set of recipients' email addresses
     */
    public void setRecipients(final Set<Recipient> pRecipients) {
        recipients = pRecipients;
    }

    /**
     * Get email
     *
     * @return the email
     */
    public Email getEmail() {
        return email;
    }

    /**
     * Set email
     *
     * @param pEmail
     *            The email
     */
    public void setEmail(final Email pEmail) {
        email = pEmail;
    }

}
