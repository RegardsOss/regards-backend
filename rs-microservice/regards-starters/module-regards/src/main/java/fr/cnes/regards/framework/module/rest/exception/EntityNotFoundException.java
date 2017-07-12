/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Class EntityNotFoundException
 *
 * Exception to indicates that the required entity is not found.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 *
 */
public class EntityNotFoundException extends EntityException {

    public EntityNotFoundException(final String pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity %s with id : %s doesn't exists", pEntityClass.getName(), pEntityIdentifier));
    }

    public EntityNotFoundException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        this(String.valueOf(pEntityIdentifier), pEntityClass);
    }

    /**
     * @param pEntityId
     */
    public EntityNotFoundException(Long pEntityIdentifier) {
        this(String.valueOf(pEntityIdentifier));
    }

    /**
     * @param pEntityIpId
     */
    public EntityNotFoundException(String pEntityIpId) {
        super(String.format("Entity with ipId : %s doesn't exists", pEntityIpId));
    }
}
