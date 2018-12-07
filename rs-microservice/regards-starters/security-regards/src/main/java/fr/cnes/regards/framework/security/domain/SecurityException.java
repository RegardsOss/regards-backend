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
package fr.cnes.regards.framework.security.domain;

/**
 * This exception is thrown when an error occurs during security check process
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
public class SecurityException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -3031544861738056012L;

    /**
     * Constructor
     * @param pMessage the message
     * @param pCause the cause
     */
    public SecurityException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }
}
