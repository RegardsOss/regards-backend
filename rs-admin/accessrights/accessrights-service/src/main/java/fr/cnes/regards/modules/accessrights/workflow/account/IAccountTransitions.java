/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;

/**
 * State pattern implementation for defining the actions managing the state of an account.<br>
 * Default implementation is to throw an exception stating that the called action is not allowed for the account's
 * current status.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public interface IAccountTransitions {

    /**
     * Passes the account from status PENDING to ACCEPTED or REFUSED according to the account's acceptance policy.
     *
     * @param pAccessRequestDto
     *            The DTO containing all information to create the new {@link Account}
     * @param pValidationUrl
     *            The validation url for the account confirmation email
     * @throws EntityException
     *             <br>
     *             {@link EntityAlreadyExistsException} Thrown when an account with same <code>email</code> already
     *             exists<br>
     *             {@link EntityTransitionForbiddenException} Thrown when the account is not in status PENDING<br>
     * @return the created account
     */
    default Account requestAccount(final AccessRequestDto pAccessRequestDto, final String pValidationUrl)
            throws EntityException {
        throw new EntityTransitionForbiddenException(pAccessRequestDto.getEmail(), Account.class, null,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes the account from status PENDING to ACCEPTED.
     *
     * @param pAccount
     *            The {@link Account}
     * @param pValidationUrl
     *            The validation url for the account confirmation email
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} Thrown when the account is not in status PENDING<br>
     *             {@link EntityNotFoundException} Thrown when the email validation template could not be found
     */
    default void acceptAccount(final Account pAccount, final String pValidationUrl) throws EntityException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(), Account.class,
                pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Validate an ACCEPTED account via email and passes it to ACTIVE status.
     *
     * @param pVerificationToken
     *            The {@link VerificationToken}, embedding the account
     * @throws EntityOperationForbiddenException
     *             Thrown when the code does not match the account's <code>code</code> field<br>
     *             {@link EntityTransitionForbiddenException} Thrown when the account is not in status ACCEPOTED<br>
     */
    default void validateAccount(final VerificationToken pVerificationToken) throws EntityOperationForbiddenException {
        throw new EntityTransitionForbiddenException(pVerificationToken.getAccount().getId().toString(), Account.class,
                pVerificationToken.getAccount().getStatus().toString(),
                Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes an ACTIVE account to the status LOCKED.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws EntityTransitionForbiddenException
     *             Thrown when the account is not in status LOCKED
     */
    default void lockAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(), Account.class,
                pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Unlocks a LOCKED account.
     *
     * @param pAccount
     *            The {@link Account}
     * @param pUnlockCode
     *            The unlock code. Must match to account's <code>code</code> field
     * @throws EntityOperationForbiddenException
     *             Thrown when the code does not match the account's <code>code</code> field<br>
     *             {@link EntityTransitionForbiddenException} Thrown when the account is not in status LOCKED<br>
     *
     */
    default void unlockAccount(final Account pAccount, final String pUnlockCode)
            throws EntityOperationForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(), Account.class,
                pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes an ACTIVE account to the status INACTIVE.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws EntityTransitionForbiddenException
     *             Thrown when the account is not in status INACTIVE
     */
    default void inactiveAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(), Account.class,
                pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Passes an INACTIVE account to the status ACTIVE.
     *
     * @param pAccount
     *            The {@link Account}
     * @throws EntityTransitionForbiddenException
     *             Thrown when the account is not in status ACTIVE
     */
    default void activeAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(), Account.class,
                pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    };

    /**
     * Remove an {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param pAccount
     *            The account
     * @throws ModuleException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.<br>
     *             {@link EntityTransitionForbiddenException} Thrown if the {@link Account} is not in state ACTIVE.
     */
    default void deleteAccount(final Account pAccount) throws ModuleException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(), Account.class,
                pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    };
}
