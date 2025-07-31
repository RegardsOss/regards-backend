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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.gson.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;

/**
 * Utility to read attributes in a Gson object by their json path.
 * This class is a wrapper over jayway's JsonPath class, configured for the gson library.
 *
 * @author Julien Canches
 */
public final class GsonPathUtils {

    private static final ParseContext JSON_PATH_PARSE_CONTEXT = JsonPath.using(Configuration.builder()
                                                                                            .jsonProvider(new GsonJsonProvider())
                                                                                            .build());

    private GsonPathUtils() {
    }

    /**
     * Extracts the string attribute from the given JSON payload using the given JSON path.
     *
     * @param payload  The root Json object that is searched
     * @param jsonPath A json path. Example: <code>$.content.items[12].name</code>. See {@link JsonPath} for more
     *                 details.
     * @throws com.jayway.jsonpath.PathNotFoundException If no attribute with the specified path exists
     */
    public static String getString(JsonObject payload, String jsonPath) {
        return JSON_PATH_PARSE_CONTEXT.parse(payload).<JsonElement>read(jsonPath).getAsString();
    }

}
