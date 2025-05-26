/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.service;

import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountAcceptedEvent;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Define the base interface for any implementation of an Account Service.
 *
 * @author CS SI
 */
public interface IAccountService {

    /**
     * Create an account.
     *
     * @param account     The {@link Account}
     * @param project     An optional project name that the account should be linked to
     * @param originUrl   The origin URL (used to redirect the user once they have confirmed their email). This
     *                    parameter is required only when the account status is left null or is set to
     *                    {@link AccountStatus#EMAIL_VERIFICATION}.
     * @param requestLink The request link (also used to redirect the user once they have confirmed their email).
     *                    This parameter is required only when the account status is left null or is set to
     *                    {@link AccountStatus#EMAIL_VERIFICATION}.
     * @return The account
     */
    Account createAccount(Account account,
                          @Nullable String project,
                          @Nullable String originUrl,
                          @Nullable String requestLink) throws EntityInvalidException;

    /**
     * Sends a new validation email for the specified account.
     * The initial email is automatically sent upon account creation, but a new email
     * can be sent if the validation token has expired or if the user has lost or not received
     * previous emails.
     *
     * @throws EntityOperationForbiddenException when the account email has already been confirmed by the user
     */
    void resendVerificationEmail(Account account) throws EntityOperationForbiddenException;

    /**
     * Set Account status to {@link AccountStatus#ACTIVE}
     * and publish an {@link AccountAcceptedEvent}
     *
     * @param account The {@link Account}
     */
    void activate(Account account);

    /**
     * Retrieve a list of all {@link Account}s, with optional search parameters.
     *
     * @param parameters search parameters
     * @param pageable   paging information
     * @return A list of accounts
     */
    Page<Account> retrieveAccountList(AccountSearchParameters parameters, Pageable pageable);

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param pAccountId The {@link Account}'s <code>id</code>
     * @return The account
     * @throws EntityNotFoundException Thrown if no {@link Account} with passed <code>id</code> could be found
     */
    Account retrieveAccount(Long pAccountId) throws EntityNotFoundException;

    /**
     * Retrieve the {@link Account} of passed <code>email</code>
     *
     * @param pEmail The {@link Account}'s <code>email</code>
     * @return the account
     * @throws EntityNotFoundException Thrown if no {@link Account} with passed <code>email</code> could be found
     */
    Account retrieveAccountByEmail(String pEmail) throws EntityNotFoundException;

    /**
     * Return <code>true</code> if an {@link Account} of passed <code>id</code> exists.
     *
     * @param pId The {@link Account}'s <code>id</code>
     * @return <code>true</code> if exists, else <code>false</code>
     */
    boolean existAccount(Long pId);

    /**
     * Return <code>true</code> if an {@link Account} of passed <code>email</code> exists.
     *
     * @param pEmail The {@link Account}'s <code>email</code>
     * @return <code>true</code> if exists, else <code>false</code>
     */
    boolean existAccount(String pEmail);

    /**
     * Update an {@link Account} with passed values. Passwords and emails are not updated by this method.
     *
     * @param pAccountId      The <code>id</code> of the {@link Account} to update
     * @param pUpdatedAccount The new values to set
     * @return the {@link Account} created
     * @throws EntityException <br>
     *                         {@link EntityInconsistentIdentifierException} Thrown when <code>pAccountId</code> is different from
     *                         the id of <code>pUpdatedAccount</code><br>
     *                         {@link EntityNotFoundException} Thrown when no {@link Account} could be found with id
     *                         <code>pAccountId</code><br>
     */
    Account updateAccount(Long pAccountId, Account pUpdatedAccount) throws EntityException;

    /**
     * Return <code>true</code> if the passed <code>pPassword</code> is equal to the one set on the {@link Account} of
     * passed <code>email</code>
     *
     * @param email                The {@link Account}'s <code>email</code>
     * @param password             The password to check
     * @param checkAccountValidity if true, this method check also the account validity
     * @return <code>true</code> if the password is valid, else <code>false</code>
     * @throws EntityNotFoundException Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     */
    boolean validatePassword(String email, String password, boolean checkAccountValidity)
        throws EntityNotFoundException;

    /**
     * Validate the password according to the regex provided by file. Mainly used by create and update methods so an
     * invalid password wouldn't be used
     *
     * @param pNewAccount the account which we are checking the password from
     * @throws EntityInvalidException thrown if the provided password does not respect the configured regex
     */
    void checkPassword(Account pNewAccount) throws EntityInvalidException;

    /**
     * @param pPassword the password to validate
     * @return whether the password respect the regex
     */
    boolean validPassword(String pPassword);

    /**
     * @return password rules
     */
    String getPasswordRules();

    /**
     * Check account validity
     */
    void checkAccountValidity();

    /**
     * Change password
     *
     * @param pId              account id
     * @param pEncryptPassword encrypted password
     * @throws EntityNotFoundException if no account of passed id could be found
     */
    void changePassword(Long pId, String pEncryptPassword) throws EntityNotFoundException;

    /**
     * Allows to reset an account Authentication Failed Counter
     *
     * @param id account id
     * @throws EntityNotFoundException if no account exists with this id
     */
    void resetAuthenticationFailedCounter(Long id) throws EntityNotFoundException;

    /**
     * Lists all possible origins (Service Provider or Regards) for any account
     *
     * @return list of origins as String
     */
    List<String> getOrigins();

    /**
     * Link a project to an account
     *
     * @param email   email of the account to link
     * @param project name of the project to link
     * @throws EntityException if either account or project is invalid
     */
    void link(String email, String project) throws EntityException;

    /**
     * Unlink a project from an account
     *
     * @param email   email of the account to link
     * @param project name of the project to link
     * @throws EntityException if either account or project is invalid
     */
    void unlink(String email, String project) throws EntityException;

    void updateOrigin(String email, String origin) throws EntityException;

}
