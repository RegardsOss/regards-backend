/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account.passwordreset;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Event transporting the data needed for the password reset process.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnPasswordResetEvent extends ApplicationEvent {

    /**
     * Generated serial
     */
    private static final long serialVersionUID = -7099682370525387294L;

    /**
     * The initiator's account
     */
    private Account account;

    /**
     * The url of the app from where was issued the query
     */
    private final String originUrl;

    /**
     * The url to redirect the user to the password reset interface
     */
    private final String requestLink;

    /**
     * Class constructor
     *
     * @param pAccount
     *            the account
     * @param pOriginUrl
     *            the origin url
     * @param pResetUrl
     *            the reset url
     */
    public OnPasswordResetEvent(final Account pAccount, final String pOriginUrl, final String pResetUrl) {
        super(pAccount);
        this.account = pAccount;
        this.originUrl = pOriginUrl;
        this.requestLink = pResetUrl;
    }

    /**
     * @return the account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * @param pAccount
     *            the account to set
     */
    public void setAccount(final Account pAccount) {
        account = pAccount;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @return the resetUrl
     */
    public String getRequestLink() {
        return requestLink;
    }

}