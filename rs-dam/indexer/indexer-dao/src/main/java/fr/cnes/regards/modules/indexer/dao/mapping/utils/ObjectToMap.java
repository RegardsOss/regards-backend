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
package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting Java objects to Map<String, Object> using Gson,
 * without serializing/deserializing JSON text.
 *
 * @author Julien Canches
 */
public final class ObjectToMap {

    private ObjectToMap() {
    }

    /**
     * Converts any object to a Map<String, Object> efficiently using Gson.
     * No intermediate JSON string is created.
     */
    public static Map<String, Object> toMap(Gson gson, Object src) {
        JsonElement tree = gson.toJsonTree(src);
        if (!tree.isJsonObject()) {
            throw new IllegalArgumentException(String.format("Object [%s] must be serializable to a JSON object",
                                                             src.getClass()));
        }
        return toMap(tree.getAsJsonObject());
    }

    private static Map<String, Object> toMap(JsonObject jsonObj) {
        Map<String, Object> map = new LinkedHashMap<>(); // we want to keep the serialization order
        for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
            map.put(entry.getKey(), fromJsonElement(entry.getValue()));
        }
        return map;
    }

    private static Object fromJsonElement(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) {
                return p.getAsBoolean();
            }
            if (p.isNumber()) {
                return getNumber(p);
            }
            return p.getAsString();
        }
        if (el.isJsonArray()) {
            JsonArray array = el.getAsJsonArray();
            List<Object> list = new ArrayList<>(array.size());
            for (JsonElement child : array) {
                list.add(fromJsonElement(child));
            }
            return list;
        }
        if (el.isJsonObject()) {
            return toMap(el.getAsJsonObject());
        }
        return null; // Unreachable
    }

    @NotNull
    @SuppressWarnings("java:S1067") // multiple conditions are easier here
    private static Number getNumber(JsonPrimitive p) {
        Number n = p.getAsNumber();
        // If the number object is a primitive wrapper, keep it as-is:
        if (n instanceof Long
            || n instanceof Integer
            || n instanceof Float
            || n instanceof Double
            || n instanceof Short
            || n instanceof Byte
            || n instanceof BigDecimal) {
            return n;
        }
        // Otherwise,it is an exotic implementation like Gson's LazilyParserNumber.
        // If so, use its double value, or even long value if the double
        // happens to be convertable to a long without losing information
        double d = n.doubleValue();
        if (Double.isFinite(d) && !Double.isNaN(d)) {
            long l = (long) d;
            // Caution: do not follow IntelliJ's advice to simplify the if statement, we need the explicit cast
            if (l == d) {
                return l;
            }
        }
        return d;
    }
}
