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
package fr.cnes.regards.modules.accessrights.domain;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Enumerates possible values for {@link ProjectUser#getStatus()}
 *
 * @author Xavier-Alexandre Brochard
 */
public enum UserStatus {

    /**
     * The associated account is active, waiting for the project administrator to approve.
     * This status is skipped in auto-approve mode.
     * Previous status: {@link #WAITING_ACCOUNT_ACTIVE}.
     * Next status: {@link #ACCESS_GRANTED} (when approved by the project administrator)
     * Next status: {@link #ACCESS_DENIED} (when refused by the project administrator)
     */
    WAITING_ACCESS,
    /**
     * The user has requested access to the project, but it was denied by the project
     * administrator.
     * Previous status: {@link #WAITING_ACCESS}
     * Next status: {@link #ACCESS_GRANTED} (if the project administrator changes their mind and approves the user)
     */
    ACCESS_DENIED,
    /**
     * The user was approved by the project administrator and can access the project.
     * Previous status: {@link #WAITING_ACCESS} or {@link #ACCESS_DENIED}
     */
    ACCESS_GRANTED,
    /**
     * The user access has be disabled by the project administrator.
     * Previous status: {@link #ACCESS_GRANTED}
     * Next status: {@link #ACCESS_GRANTED} (when the project administrator reactivates the account)
     */
    ACCESS_INACTIVE,
    /**
     * The associated account is not active yet (waiting for super administrator approval
     * or email verification).
     * This is the initial state.
     * Next status: {@link #WAITING_ACCESS} (when the account activation is received from admin-instance)
     */
    WAITING_ACCOUNT_ACTIVE;
}
