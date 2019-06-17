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
package fr.cnes.regards.framework.jpa.exception;

/**
 * Class MultiDataBasesException
 *
 * Exception raised when there is an error in the multitenancy databases access
 * @author CS
 */
public class MultiDataBasesException extends Exception {

    /**
     * serialVersionUID field.
     * @author CS
     */
    private static final long serialVersionUID = 7382111289929689769L;

    /**
     * Constructor
     * @param pMessage message
     * @param pCause cause
     */
    public MultiDataBasesException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     * Constructor
     * @param pMessage message
     */
    public MultiDataBasesException(final String pMessage) {
        super(pMessage);
    }

    /**
     * Constructor
     * @param pCause cause
     */
    public MultiDataBasesException(final Throwable pCause) {
        super(pCause);
    }

}
