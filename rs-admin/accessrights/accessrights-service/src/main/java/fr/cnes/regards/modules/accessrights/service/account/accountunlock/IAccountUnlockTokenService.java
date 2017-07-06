/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account.accountunlock;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Service managing the account unlock tokens
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccountUnlockTokenService {

    /**
     * Retrieve the {@link AccountUnlockToken} of passed token string
     *
     * @param pToken
     *            the account
     * @return the token
     * @throws EntityNotFoundException
     *             if no {@link AccountUnlockToken} with passed token could be found
     */
    AccountUnlockToken findByToken(final String pToken) throws EntityNotFoundException;

    /**
     * Create a {@link AccountUnlockToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @return generated token
     */
    String create(Account pAccount);

    /**
     * Delete all {@link AccountUnlockToken}s for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pToken
     *            the token
     */
    void deleteAllByAccount(Account pAccount);

}
