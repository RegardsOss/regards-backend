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
 * Exception to be thrown when an Entity which is embedded into another one cannot be found
 *
 * This is a RuntimeException so we don't need a DTO to handle this kind of issue and we can still keep track of the
 * exception for a proper handling on the REST layer
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
// Seems unused!
@Deprecated
public class EntityEmbeddedEntityNotFoundException extends RuntimeException {

    public EntityEmbeddedEntityNotFoundException(Throwable pCause) {
        super(pCause);
    }

}