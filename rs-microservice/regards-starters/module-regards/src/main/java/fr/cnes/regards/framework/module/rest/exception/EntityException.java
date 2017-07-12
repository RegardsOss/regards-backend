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
 * Class EntityException
 *
 * Global entities exceptions
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class EntityException extends ModuleException {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = -6284091634593228162L;

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            Entity error message
     * @since 1.0-SNAPSHOT
     */
    public EntityException(final String pMessage) {
        super(pMessage);
    }

}
