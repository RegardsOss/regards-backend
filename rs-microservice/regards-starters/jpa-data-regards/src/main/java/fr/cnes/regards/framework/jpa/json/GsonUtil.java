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
package fr.cnes.regards.framework.jpa.json;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

/**
 *
 * Utility class allowing us to serialize and deserialize object to and from JSON
 *
 * inspired from @Vlad Mihalcea JacksonUtil class
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
public final class GsonUtil {

    /**
     * Json Mapper
     */
    private static Gson gson;

    private GsonUtil() {
        // Overriding public constructor
    }

    /**
     * Convert JSON string to JAVA object
     * @param <T> java object type
     * @param json JSON string to deserialize
     * @param type target type
     * @return a JAVA object representing the target type
     */
    public static <T> T fromString(String json, Type type) {
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException(
                    "The given string value: " + json + " cannot be transformed to Json object", e);
        }
    }

    /**
     * Convert JAVA object to JSON string
     * @param object java object to convert
     * @return JSON string
     */
    public static String toString(Object object) {
        return gson.toJson(object);
    }

    /**
     * Convert JAVA object to JSON element
     * @param object java object to convert
     * @return {@link JsonElement}
     */
    public static JsonElement toJsonNode(final Object object) {
        return gson.toJsonTree(object);
    }

    /**
     * Clone a java object
     * @param <T> java object type
     * @param object java object to clone
     * @param type target type
     * @return java object clone
     */
    public static <T> T clone(T object, Type type) {
        return fromString(toString(object), type);
    }

    /**
     * Initialize utility class with common GSON instance managing system custom adapters and factories
     * @param gsonInstance GSON system instance
     */
    public static void setGson(Gson gsonInstance) {
        gson = gsonInstance;
    }

    /**
     * Create a {@link TypeToken} for a Map<K, V>, that can be used as a helper for deserialization
     * @param keyArgType {@link Type} of the key type of the map
     * @param argType {@link Type} of the value type of the map
     * @return a TypeToken representing a Map<K,V>, K and V being dynamicly set
     */
    public static <K, V> TypeToken<Map<K, V>> createMapTypeToken(Type keyArgType, Type argType) {
        return new TypeToken<Map<K, V>>() {

        }.where(new TypeParameter<K>() {

        }, (TypeToken<K>) TypeToken.of(keyArgType)).where(new TypeParameter<V>() {

        }, (TypeToken<V>) TypeToken.of(argType));
    }
}
