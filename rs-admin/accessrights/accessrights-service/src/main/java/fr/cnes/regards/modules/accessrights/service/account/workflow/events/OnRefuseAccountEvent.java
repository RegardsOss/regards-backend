/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account.workflow.events;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Event fired when an account is refused.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnRefuseAccountEvent extends ApplicationEvent {

    /**
     * The account
     */
    private Account account;

    /**
     * @param pSource
     * @param pAccount
     */
    public OnRefuseAccountEvent(Account pAccount) {
        super(pAccount);
        account = pAccount;
    }

    /**
     * @return the account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * @param pAccount the account to set
     */
    public void setAccount(Account pAccount) {
        account = pAccount;
    }

}