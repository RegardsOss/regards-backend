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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import org.springframework.stereotype.Component;

/**
 * Provider class returning the right {@link IAccountTransitions} for the passed {@link Account} according to its
 * <code>state</code> field.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class AccountStateProvider {

    /**
     * Email verification state
     */
    private final EmailVerificationState emailVerificationState;

    /**
     * Pending state
     */
    private final PendingState pendingState;

    /**
     * Active state
     */
    private final ActiveState activeState;

    /**
     * Inactive state
     */
    private final InactiveState inactiveState;

    /**
     * Locked state
     */
    private final LockedState lockedState;

    public AccountStateProvider(EmailVerificationState emailVerificationState,
                                PendingState pendingState,
                                ActiveState activeState,
                                InactiveState inactiveState,
                                LockedState lockedState) {
        this.emailVerificationState = emailVerificationState;
        this.pendingState = pendingState;
        this.activeState = activeState;
        this.inactiveState = inactiveState;
        this.lockedState = lockedState;
    }

    /**
     * Get the right account state based on the passed status
     *
     * @param pStatus The account status
     * @return the account state object
     */
    private IAccountTransitions getState(final AccountStatus status) {
        return switch (status) {
            case INACTIVE -> inactiveState;
            case LOCKED -> lockedState;
            case PENDING, INACTIVE_PASSWORD -> pendingState;
            case ACTIVE -> activeState;
            case EMAIL_VERIFICATION -> emailVerificationState;
        };
    }

    /**
     * Get the right account state based on the passed account's status
     *
     * @param pAccount The account
     * @return the account state object
     */
    public IAccountTransitions getState(final Account pAccount) {
        return getState(pAccount.getStatus());
    }

}
