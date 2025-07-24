/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger to log modification on properties
 *
 * @author Kevin Marchois
 */
public final class PropertyPatchLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyPatchLogger.class);

    private static final String PREFIX = "[MONITORING] ";

    private static final String FORMAT = PREFIX + "Feature PATCHED | {} | {} | {} | {} => {}";

    private PropertyPatchLogger() {
    }

    public static void logPropertyUpdated(String modifier,
                                          String identifier,
                                          String key,
                                          Object oldValue,
                                          Object newValue) {
        LOGGER.debug(FORMAT, modifier, identifier, key, oldValue, newValue);
    }

    public static void logPropertyAdded(String modifier, String identifier, String key, Object newValue) {
        LOGGER.debug(FORMAT, modifier, identifier, key, "none", newValue);
    }

    public static void logPropertyRemoved(String modifier, String identifier, String key, Object oldValue) {
        LOGGER.debug(FORMAT, modifier, identifier, key, oldValue, "none");
    }

}