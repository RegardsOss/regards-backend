/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when the email validation of an {@link String} was sucessfully performed.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnAccountEmailValidationEvent extends ApplicationEvent {

    /**
     * The email of the account
     */
    private String email;

    /**
     * @param pEmail the email of the account
     */
    public OnAccountEmailValidationEvent(final String pEmail) {
        super(pEmail);
        email = pEmail;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param pEmail the email to set
     */
    public void setEmail(String pEmail) {
        email = pEmail;
    }

}