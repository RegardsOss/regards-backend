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
package fr.cnes.regards.framework.authentication.internal;

/**
 * Enumeration for authentication plugins status.
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public enum AuthenticationStatus {

    /**
     * Authentication is successful, access is granted
     */
    ACCESS_GRANTED,

    /**
     * Login error login or password invalid.
     */
    ACCOUNT_UNKNOWN,

    /**
     * Account is not validated by administrator yet
     */
    ACCOUNT_PENDING,

    /**
     * Account inactive
     */
    ACCOUNT_INACTIVE,

    /**
     * Account inactive password out of date
     */
    ACCOUNT_INACTIVE_PASSWORD,

    /**
     * Password locked.
     */
    ACCOUNT_LOCKED,

    /**
     * Account does not have a user for the request project
     */
    USER_UNKNOWN,

    /**
     * Waiting for the account to be active
     */
    USER_WAITING_ACCOUNT_ACTIVE,

    /**
     * User access to project is not validated yet
     */
    USER_WAITING_ACCESS,

    /**
     * Waiting for the user to click on the link in the verification email
     */
    USER_WAITING_EMAIL_VERIFICATION,

    /**
     * User access denied.
     */
    USER_ACCESS_DENIED,

    /**
     * User access inactive
     */
    USER_ACCESS_INACTIVE,

    /**
     * User cannot access instance tenant
     */
    INSTANCE_ACCESS_DENIED;

    @Override
    public String toString() {
        return this.name();
    }

}
