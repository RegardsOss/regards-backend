/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;

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
     * Passes the account from status PENDING to ACTIVE.
     *
     * @param pAccount The {@link Account}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} Thrown when the account is not in status PENDING<br>
     *                         {@link EntityNotFoundException} Thrown when the email validation template could not be found
     */
    default void acceptAccount(final Account pAccount) throws EntityException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Deletes a PENDING account.
     *
     * @param pAccount The {@link Account}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} Thrown when the account is not in status PENDING<br>
     *                         {@link EntityNotFoundException} Thrown when the email validation template could not be found
     */
    default void refuseAccount(final Account pAccount) throws EntityException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an ACTIVE account to the status LOCKED.
     *
     * @param pAccount The {@link Account}
     * @throws EntityTransitionForbiddenException Thrown when the account is not in status LOCKED
     */
    default void lockAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Send to the user an email containing a link with limited validity to unlock its account.
     *
     * @param pAccount     The {@link Account}
     * @param pOriginUrl   The origin url
     * @param pRequestLink The request link
     * @throws EntityOperationForbiddenException Thrown when the code does not match the account's <code>code</code> field<br>
     *                                           {@link EntityTransitionForbiddenException} Thrown when the account is not in status LOCKED<br>
     */
    default void requestUnlockAccount(final Account pAccount, final String pOriginUrl, final String pRequestLink)
        throws EntityOperationForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Unlocks a LOCKED account.
     *
     * @param pAccount The {@link Account}
     * @param pToken   The unlock token. Must match to account's <code>code</code> field
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} when the account or the token does not exist<br>
     *                         {@link EntityOperationForbiddenException} when the token is invalid<br>
     *                         {@link EntityTransitionForbiddenException} when the account is not in status LOCKED<br>
     */
    default void performUnlockAccount(final Account pAccount, final String pToken) throws EntityException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an ACTIVE account to the status INACTIVE.
     *
     * @param pAccount The {@link Account}
     * @throws EntityTransitionForbiddenException Thrown when the account is not in status INACTIVE
     */
    default void inactiveAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an INACTIVE account to the status ACTIVE.
     *
     * @param pAccount The {@link Account}
     * @throws EntityTransitionForbiddenException Thrown when the account is not in status ACTIVE
     */
    default void activeAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Remove an {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param pAccount The account
     * @throws ModuleException Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.<br>
     *                         {@link EntityTransitionForbiddenException} Thrown if the {@link Account} is not in state ACTIVE.
     */
    default void deleteAccount(final Account pAccount) throws ModuleException {
        throw new EntityTransitionForbiddenException(pAccount.getId().toString(),
                                                     Account.class,
                                                     pAccount.getStatus().toString(),
                                                     Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Can we delete this account? An account is considered deletable if there is no project user linked to it
     *
     * @param pAccount the account
     * @return <code>true</code> if we can, else <code>false</code>
     */
    default boolean canDelete(final Account pAccount) { // NOSONAR
        return false;
    }
}
