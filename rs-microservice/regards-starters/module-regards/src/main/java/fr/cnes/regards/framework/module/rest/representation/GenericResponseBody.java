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
package fr.cnes.regards.framework.module.rest.representation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Generic response body with containing a single message and key value pairs
 *
 * @author Marc Sordi
 *
 */
public class GenericResponseBody {

    private String message;

    private final Map<String, Object> properties = new HashMap<>();

    public GenericResponseBody() {
        // Default constructor
    }

    public GenericResponseBody(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setMessage(String pMessage) {
        message = pMessage;
    }
}
