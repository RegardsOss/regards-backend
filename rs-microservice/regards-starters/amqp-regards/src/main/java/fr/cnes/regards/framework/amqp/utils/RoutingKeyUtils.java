/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.utils;

/**
 * Utils methods to build routing keys
 *
 * @author Thibaud Michaudel
 **/
public class RoutingKeyUtils {

    private RoutingKeyUtils() {
    }

    /**
     * Build routing key from the application name and the request id
     */
    public static String buildRequestIdFromId(String applicationName, Long id) {
        return applicationName + "." + id;
    }

    /**
     * Retrieve the id from a routing key using the application name
     */
    public static Long buildIdFromRequestId(String applicationName, String requestId) {
        return Long.valueOf(requestId.split(applicationName + ".")[1]);
    }
}
