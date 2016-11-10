/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.instance;

import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * State pattern implemention for defining the actions managing the state of an account.<br>
 * Default implementation is to throw an exception stating that the called action is not allowed for the account's
 * current status.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
interface IAccountState {

    /**
     * Passes the account from status PENDING to ACCEPTED or REFUSED according to the account's acceptance policy.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not in status PENDING
     */
    default void makeAdminDecision(final Account pAccount) throws IllegalActionForAccountStatusException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Validate an ACCEPTED account via email and passes it to ACTIVE status.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not in status ACCEPTED
     */
    default void emailValidation(final Account pAccount) throws IllegalActionForAccountStatusException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes an ACTIVE account to the status LOCKED.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not in status LOCKED
     */
    default void lockAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Unlocks a LOCKED account.
     *
     * @param pAccount
     *            The {@link Account}
     * @param pUnlockCode
     *            The unlock code. Must match to account's <code>code</code> field
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not in status LOCKED
     * @throws InvalidValueException
     *             Thrown when the code does not match the account's <code>code</code> field
     */
    default void unlockAccount(final Account pAccount, final String pUnlockCode)
            throws IllegalActionForAccountStatusException, InvalidValueException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes an ACTIVE account to the status INACTIVE.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not in status INACTIVE
     */
    default void inactiveAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes an INACTIVE account to the status ACTIVE.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not in status ACTIVE
     */
    default void activeAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Remove an {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param pAccount
     *            The account
     * @throws OperationForbiddenException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.<br>
     *             {@link IllegalActionForAccountStatusException} Thrown if the {@link Account} is not in state ACTIVE.
     */
    default void delete(final Account pAccount) throws OperationForbiddenException {
        throw new IllegalActionForAccountStatusException(pAccount,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };
}
