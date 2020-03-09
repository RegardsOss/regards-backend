/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * Exception indicating a forbidden operation on an entity
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
@SuppressWarnings("serial")
public class EntityOperationForbiddenException extends EntityException {

    public EntityOperationForbiddenException(final String entityIdentifier, final Class<?> entityClass,
            final String message) {
        super(String.format("Operation on entity \"%s\" with id: \"%s\" is forbidden: %s", entityClass.getName(),
                            entityIdentifier, message));
    }

    public EntityOperationForbiddenException(String message) {
        super(message);
    }
}
