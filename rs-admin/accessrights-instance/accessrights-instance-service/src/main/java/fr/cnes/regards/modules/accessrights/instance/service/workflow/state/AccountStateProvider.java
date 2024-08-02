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
import org.springframework.beans.factory.annotation.Autowired;
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
     * Pending state
     */
    @Autowired
    private PendingState pendingState;

    /**
     * Active state
     */
    @Autowired
    private ActiveState activeState;

    /**
     * Inactive state
     */
    @Autowired
    private InactiveState inactiveState;

    /**
     * Locked state
     */
    @Autowired
    private LockedState lockedState;

    /**
     * Get the right account state based on the passed status
     *
     * @param pStatus The account status
     * @return the account state object
     */
    public IAccountTransitions getState(final AccountStatus pStatus) {
        final IAccountTransitions state;
        switch (pStatus) {
            case ACTIVE:
                state = activeState;
                break;
            case INACTIVE:
                state = inactiveState;
                break;
            case LOCKED:
                state = lockedState;
                break;
            case PENDING:
            default:
                state = pendingState;
                break;
        }
        return state;
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
