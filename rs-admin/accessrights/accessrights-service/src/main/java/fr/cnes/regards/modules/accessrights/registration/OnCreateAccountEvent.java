/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * When an account was successfully created in status PENDING, we want to store the data passed from the fronted
 * (originUrl and requestLink) in the verification token in order to put them in the email validation link once the
 * account is accepted by admin.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnCreateAccountEvent extends ApplicationEvent {

    /**
     * Serial
     */
    private static final long serialVersionUID = 2224468625344390929L;

    /**
     * The account
     */
    private Account account;

    /**
     * The origin url
     */
    private String originUrl;

    /**
     * The redirection link when the user clicks on the link in the mail
     */
    private String requestLink;

    /**
     * @param pAccount
     *            The account
     * @param pOriginUrl
     *            The origin url
     * @param pRequestLink
     *            The redirection link when the user clicks on the link in the mail
     */
    public OnCreateAccountEvent(final Account pAccount, final String pOriginUrl, final String pRequestLink) {
        super(pAccount);
        account = pAccount;
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
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
     * @param pOriginUrl
     *            the originUrl to set
     */
    public void setOriginUrl(final String pOriginUrl) {
        originUrl = pOriginUrl;
    }

    /**
     * @return the requestLink
     */
    public String getRequestLink() {
        return requestLink;
    }

    /**
     * @param pRequestLink
     *            the requestLink to set
     */
    public void setRequestLink(final String pRequestLink) {
        requestLink = pRequestLink;
    }

}