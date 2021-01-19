/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.instance.service.passwordreset;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PasswordResetToken;

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
