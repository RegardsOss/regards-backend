/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.role;

/**
 * Lists the default roles available.
 *
 * @author Xavier-Alexandre Brochard
 */
public enum DefaultRole {

    /**
     * Cross tenant role / useful for cross tenant endpoints
     */
    // FIXME do not accept a new role with this name
    INSTANCE_ADMIN,

    /**
     * Tenant main administrator
     */
    PROJECT_ADMIN,

    /**
     * Tenant administrator
     */
    ADMIN,

    /**
     * Tenant registered user
     */
    REGISTERED_USER,

    /**
     * Public
     */
    PUBLIC;
}