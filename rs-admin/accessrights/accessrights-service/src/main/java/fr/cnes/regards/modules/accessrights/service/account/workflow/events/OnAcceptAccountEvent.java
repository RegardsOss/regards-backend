/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account.workflow.events;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an account was accepted.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnAcceptAccountEvent extends ApplicationEvent {

    /**
     * The email of the account
     */
    private String email;

    /**
     * @param pEmail the email of the account
     */
    public OnAcceptAccountEvent(final String pEmail) {
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