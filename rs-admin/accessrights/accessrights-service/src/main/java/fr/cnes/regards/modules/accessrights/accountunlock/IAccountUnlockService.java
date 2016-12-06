/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.accountunlock;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Service managing the account unlock tokens
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccountUnlockService {

    /**
     * Retrieve the {@link AccountUnlockToken} of passed token string
     *
     * @param pToken
     *            the account
     * @return the token
     * @throws EntityNotFoundException
     *             if no {@link AccountUnlockToken} with passed token could be found
     */
    AccountUnlockToken getAccountUnlockToken(final String pToken) throws EntityNotFoundException;

    /**
     * Create a {@link AccountUnlockToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pToken
     *            the token
     */
    void createAccountUnlockToken(Account pAccount, String pToken);

    /**
     * Send a password reset email based on information stored in the passed event
     *
     * @param pAccount
     *            the account
     * @param pAppUrl
     *            the app url
     */
    void sendAccountUnlockEmail(Account pAccount, String pAppUrl);

}
