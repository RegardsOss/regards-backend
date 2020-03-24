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

package fr.cnes.regards.framework.utils.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception for plugin utils package. It usually means that the plugin couldn't be instanciated.
 * @author Christophe Mertz
 */
@SuppressWarnings("serial")
public class PluginUtilsRuntimeException extends RuntimeException {

    /**
     * Class Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsRuntimeException.class);

    /**
     * Constructor
     * @param message an error message
     */
    public PluginUtilsRuntimeException(final String message) {
        super(message);
        LOGGER.error(message);
    }

    /**
     * Constructor
     * @param message an error message
     * @param cause the exception
     */
    public PluginUtilsRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
        LOGGER.error(message, cause);
    }

    /**
     * Constructor
     * @param cause the exception
     */
    public PluginUtilsRuntimeException(final Throwable cause) {
        super(cause);
    }
}
