/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.passwordreset;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Event transporting the logic needed for account email validation.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnPasswordResetEvent extends ApplicationEvent {

    /**
     * Generated serial
     */
    private static final long serialVersionUID = -7099682370525387294L;

    /**
     * The app url
     */
    private final String appUrl;

    /**
     * The registered account
     */
    private final Account account;

    public OnPasswordResetEvent(final Account pAccount, final String pAppUrl) {
        super(pAccount);
        this.account = pAccount;
        this.appUrl = pAppUrl;
    }

    /**
     * @return the appUrl
     */
    public String getAppUrl() {
        return appUrl;
    }

    /**
     * @return the account
     */
    public Account getAccount() {
        return account;
    }
}