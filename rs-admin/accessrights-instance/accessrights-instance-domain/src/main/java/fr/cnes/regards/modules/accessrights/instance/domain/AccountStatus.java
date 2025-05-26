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
package fr.cnes.regards.modules.accessrights.instance.domain;

/**
 * Defines the different statuses/states for an account
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
public enum AccountStatus {
    /**
     * Account is inactive because it is past the invalidity date.
     * Next status: {@link #ACTIVE} (when reactivated by the administrator)
     */
    INACTIVE,
    /**
     * Account is inactive because the password is out of date.
     * Next status: {@link #ACTIVE} (when the user changes their password)
     */
    INACTIVE_PASSWORD,
    /**
     * Account is active.
     * Next status: {@link #LOCKED} (when the user makes too many unsuccessful login attempts)
     * Next status: {@link #INACTIVE} (when the cron task determines the account is past the invalidity date)
     * Next status: {@link #INACTIVE_PASSWORD} (when the cron task determines the password is too old)
     */
    ACTIVE,
    /**
     * Account is locked because of too many unsuccessful password attempts.
     * Next status: {@link #ACTIVE} (when the user unlocks their account through an unlock email)
     */
    LOCKED,
    /**
     * Account request is pending approval of the instance administrator.
     * The status is skipped in auto-approve mode.
     * Previous status: {@link #EMAIL_VERIFICATION}
     * Next status: {@link #ACTIVE} (when accepted by the instance administrator)
     */
    PENDING,
    /**
     * Email verification is pending.
     * This is the initial status.
     * Next status: {@link #PENDING} (when user has confirmed their email)
     */
    EMAIL_VERIFICATION
}
