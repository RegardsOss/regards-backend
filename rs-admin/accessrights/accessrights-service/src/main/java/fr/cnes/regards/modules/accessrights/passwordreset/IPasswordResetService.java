/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.passwordreset;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.PasswordResetToken;

/**
 * Service managing the password reset tokens
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IPasswordResetService {

    /**
     * Retrieve the password reset token
     *
     * @param pToken
     *            the account
     * @return the token
     * @throws EntityNotFoundException
     *             if no {@link PasswordResetToken} with passed token could be found
     */
    PasswordResetToken getPasswordResetToken(final String pToken) throws EntityNotFoundException;

    /**
     * Create a {@link PasswordResetToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pToken
     *            the token
     */
    void createPasswordResetToken(Account pAccount, String pToken);

}
