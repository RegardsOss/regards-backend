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
 * Class AlreadyExistingException
 *
 * Exception to indicates that the entity already exists.
 *
 * @deprecated use {@link EntityAlreadyExistsException}
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Deprecated
public class AlreadyExistingException extends EntityException {

    /**
     * Serial version
     */
    private static final long serialVersionUID = -2444321445173304039L;

    /**
     *
     * Constructor
     *
     * @param pEntityId
     *            entity identifier
     * @since 1.0-SNAPSHOT
     */
    public AlreadyExistingException(final String pEntityId) {
        super("Data with id : " + pEntityId + " already Exist");
    }
}
