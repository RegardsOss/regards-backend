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
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Exception indicating a parent entity cannot be deleted cause children already exist
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
@SuppressWarnings("serial")
public class EntityNotEmptyException extends EntityException {

    /**
     * Constructor setting exception message thanks to given parameters
     */
    public EntityNotEmptyException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity of type \"%s\" with id \"%s\" is not empty and cannot be removed.",
                            pEntityClass.getName(), pEntityIdentifier));
    }
}
