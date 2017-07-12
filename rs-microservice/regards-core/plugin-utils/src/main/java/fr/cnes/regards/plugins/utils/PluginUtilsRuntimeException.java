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

package fr.cnes.regards.plugins.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception for plugin utils package. It usually means that the plugin couldn't be instanciated.
 *
 * @author Christophe Mertz
 */
public class PluginUtilsRuntimeException extends RuntimeException {

    /**
     * Class Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsRuntimeException.class);

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 476540009609921511L;

    /**
     * Constructor
     *
     * @param pMessage an error message
     */
    public PluginUtilsRuntimeException(final String pMessage) {
        super(pMessage);
        LOGGER.error(pMessage);
    }

    /**
     * Constructor
     *
     * @param pMessage an error message
     * @param pCause the exception
     */
    public PluginUtilsRuntimeException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
        LOGGER.error(pMessage, pCause);
    }

    /**
     * Constructor
     *
     * @param pCause the exception
     */
    public PluginUtilsRuntimeException(final Throwable pCause) {
        super(pCause);
    }
}
