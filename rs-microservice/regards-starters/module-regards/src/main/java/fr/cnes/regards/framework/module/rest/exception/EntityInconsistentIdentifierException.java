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
 * Use on update endpoint if identifier in url path doesn't match identifier in request body
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityInconsistentIdentifierException extends EntityException {

    private static final long serialVersionUID = -2244195392447606535L;

    public EntityInconsistentIdentifierException(final Long pPathId, final Long pBodyId, final Class<?> pEntityClass) {
        // CHECKSTYLE:OFF
        super(String.format(
                            "Inconsistent entity update request for \"%s\". Path identifier \"%s\" does not match request body identifier \"%s\".",
                            pEntityClass.getName(), pPathId, pBodyId));
        // CHECKSTYLE:ON
    }

    public EntityInconsistentIdentifierException(final String pPathName, final String pBodyName,
            final Class<?> pEntityClass) {
        // CHECKSTYLE:OFF
        super(String.format(
                            "Inconsistent entity update request for \"%s\". Path identifier \"%s\" does not match request body identifier \"%s\".",
                            pEntityClass.getName(), pPathName, pBodyName));
        // CHECKSTYLE:ON
    }

}
