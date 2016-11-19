/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Define the base interface for any implementation of an Account Service.
 *
 * @author CS SI
 */
public interface IAccountService {

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @return The accounts list
     */
    List<Account> retrieveAccountList();

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown if no {@link Account} with passed <code>id</code> could be found
     * @return The account
     */
    Account retrieveAccount(Long pAccountId) throws ModuleEntityNotFoundException;

    /**
     * Retrieve the {@link Account} of passed <code>email</code>
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @return the account
     * @throws ModuleEntityNotFoundException
     *             Thrown if no {@link Account} with passed <code>email</code> could be found
     */
    Account retrieveAccountByEmail(String pEmail) throws ModuleEntityNotFoundException;

    /**
     * Return <code>true</code> if an {@link Account} of passed <code>id</code> exists.
     *
     * @param pId
     *            The {@link Account}'s <code>id</code>
     * @return <code>true</code> if exists, else <code>false</code>
     */
    boolean existAccount(Long pId);

    /**
     * Return <code>true</code> if an {@link Account} of passed <code>email</code> exists.
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @return <code>true</code> if exists, else <code>false</code>
     */
    boolean existAccount(String pEmail);

    /**
     * Update an {@link Account} with passed values.
     *
     * @param pAccountId
     *            The <code>id</code> of the {@link Account} to update
     * @param pUpdatedAccount
     *            The new values to set
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     * @throws InvalidValueException
     *             Thrown when <code>pAccountId</code> is different from the id of <code>pUpdatedAccount</code>
     */
    void updateAccount(Long pAccountId, Account pUpdatedAccount)
            throws ModuleEntityNotFoundException, InvalidValueException;

    /**
     * Send a code of type <code>pType</code> to the specified recipient.
     *
     * @param pEmail
     *            recipient's email address
     * @param pType
     *            The type of code
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed <code>email</code> could be found
     */
    void sendAccountCode(String pEmail, CodeType pType) throws ModuleEntityNotFoundException;

    /**
     * Change the passord of an {@link Account}.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @param pResetCode
     *            The reset code. Required to allow a password change
     * @param pNewPassword
     *            The new <code>password</code>
     * @throws InvalidValueException
     *             Thrown when the passed reset code is different from the one expected
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     */
    void changeAccountPassword(Long pAccountId, String pResetCode, String pNewPassword)
            throws InvalidValueException, ModuleEntityNotFoundException;

    /**
     * Return <code>true</code> if the passed <code>pPassword</code> is equal to the one set on the {@link Account} of
     * passed <code>email</code>
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @param pPassword
     *            The password to check
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     * @return <code>true</code> if the password is valid, else <code>false</code>
     */
    boolean validatePassword(String pEmail, String pPassword) throws ModuleEntityNotFoundException;

}
