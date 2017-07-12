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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional;

/**
 *
 * Class TestDaoException
 *
 * DAO Exception for tests.
 *
 * @author CS
 * @since TODO
 */
public class DaoTestException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public DaoTestException() {
        super();
    }

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            message
     * @param pCause
     *            cause
     * @since 1.0-SNAPSHOT
     */
    public DaoTestException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            message
     * @since 1.0-SNAPSHOT
     */
    public DaoTestException(String pMessage) {
        super(pMessage);
    }

}
