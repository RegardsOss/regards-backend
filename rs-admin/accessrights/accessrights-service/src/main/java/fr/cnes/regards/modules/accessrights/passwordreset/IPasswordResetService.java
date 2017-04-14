/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.passwordreset;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.PasswordResetToken;

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

    /**
     * Delete a {@link PasswordResetToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pToken
     *            the token
     */
    void deletePasswordResetTokenForAccount(Account pAccount);

    /**
     * Change the passord of an {@link Account}.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>id</code>
     * @param pResetCode
     *            The reset code. Required to allow a password change
     * @param pNewPassword
     *            The new <code>password</code>
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} Thrown when the passed reset code is different from the one
     *             expected<br>
     *             {@link EntityNotFoundException} Thrown when no {@link Account} could be found with id
     *             <code>pAccountId</code><br>
     */
    void performPasswordReset(String pAccountEmail, String pResetCode, String pNewPassword) throws EntityException;

}
