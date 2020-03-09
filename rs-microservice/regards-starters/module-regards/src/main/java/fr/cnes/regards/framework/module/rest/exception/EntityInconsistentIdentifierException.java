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
 * Exception indicating an update request is inconsistent cause URL path id not equals to body id.
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
@SuppressWarnings("serial")
public class EntityInconsistentIdentifierException extends EntityException {

    /**
     * Constructor setting the exception message thanks to the given parameters
     */
    public EntityInconsistentIdentifierException(final Long pathId, final Long bodyId, final Class<?> entityClass) {
        // CHECKSTYLE:OFF
        super(String.format(
                "Inconsistent entity update request for \"%s\". Path identifier \"%s\" does not match request body identifier \"%s\".",
                entityClass.getName(), pathId, bodyId));
        // CHECKSTYLE:ON
    }

    /**
     * Constructor setting the exception message thanks to the given parameters
     */
    public EntityInconsistentIdentifierException(final String pPathName, final String pBodyName,
            final Class<?> pEntityClass) {
        // CHECKSTYLE:OFF
        super(String.format(
                "Inconsistent entity update request for \"%s\". Path identifier \"%s\" does not match request body identifier \"%s\".",
                pEntityClass.getName(), pPathName, pBodyName));
        // CHECKSTYLE:ON
    }
}
