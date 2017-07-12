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
 * Exception to indicate that the operation is forbidden on entity.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
 */
public class EntityOperationForbiddenException extends EntityException {

    /**
     * Serial
     */
    private static final long serialVersionUID = 1056576133397279032L;

    /**
     * Creates a new {@link EntityOperationForbiddenException} with passed params.
     *
     * @param pEntityIdentifier
     *            Entity identifier
     * @param pEntityClass
     *            Entity class
     * @param pMessage
     *            Message describing the forbidden operation
     * @since 1.0-SNAPSHOT
     */
    public EntityOperationForbiddenException(final String pEntityIdentifier, final Class<?> pEntityClass,
            final String pMessage) {
        super(String.format("Operation on entity %s with id: %s is forbidden: %s", pEntityClass.getName(),
                            pEntityIdentifier, pMessage));
    }

    /**
     * Creates a new EntityForbiddenException witht he given string as message
     * 
     * @param pString
     */
    public EntityOperationForbiddenException(String pString) {
        super(pString);
    }

}
