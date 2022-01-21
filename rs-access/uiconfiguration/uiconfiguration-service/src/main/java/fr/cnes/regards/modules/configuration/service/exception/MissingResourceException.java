/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * Runtime exception thrown when a required Spring @Resource is null.
 * @author Xavier-Alexandre Brochard
 */
@SuppressWarnings("serial")
public class MissingResourceException extends RuntimeException {

    /**
     * Main exception message
     */
    private static final String MESSAGE = "Error reading layout default configuration file. Null resource or inexistent.";

    /**
     * Default constructor
     */
    public MissingResourceException() {
        super();
    }

    /**
     * @param pCause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public MissingResourceException(Throwable pCause, boolean enableSuppression, boolean writableStackTrace) {
        super(MESSAGE, pCause, enableSuppression, writableStackTrace);
    }

    /**
     * @param cause
     */
    public MissingResourceException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
