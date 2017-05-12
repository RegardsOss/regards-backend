/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json;

import java.lang.reflect.Type;

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
}
