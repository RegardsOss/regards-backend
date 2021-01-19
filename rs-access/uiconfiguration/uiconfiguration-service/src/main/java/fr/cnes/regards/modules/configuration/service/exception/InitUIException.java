/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.service.exception;

/**
 * Runtime exception thrown when an error occurs during project layout intialization.
 * @author Xavier-Alexandre Brochard
 */
@SuppressWarnings("serial")
public class InitUIException extends RuntimeException {

    /**
     * Main exception message
     */
    private static final String MESSAGE = "Error reading layout default configuration file";

    /**
     * Default constructor
     */
    public InitUIException() {
        super();
    }

    /**
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public InitUIException(Throwable pCause, boolean pEnableSuppression, boolean pWritableStackTrace) {
        super(MESSAGE, pCause, pEnableSuppression, pWritableStackTrace);
    }

    /**
     * @param pCause
     */
    public InitUIException(Throwable pCause) {
        super(MESSAGE, pCause);
    }

}
