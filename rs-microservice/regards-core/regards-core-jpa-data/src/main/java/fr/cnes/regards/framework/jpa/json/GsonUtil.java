/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json;

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
 */
public final class GsonUtil {

    /**
     * Json Mapper
     */
    private static Gson GSON;

    private GsonUtil() {
        // Overriding public constructor
    }

    public static <T> T fromString(final String pString, final Class<T> pClazz) {
        try {
            return GSON.fromJson(pString, pClazz);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException(
                    "The given string value: " + pString + " cannot be transformed to Json object", e);
        }
    }

    public static String toString(final Object pValue) {
        return GSON.toJson(pValue);
    }

    public static JsonElement toJsonNode(final Object pValue) {
        return GSON.toJsonTree(pValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T pValue) {
        return fromString(toString(pValue), (Class<T>) pValue.getClass());
    }

    /**
     * @param pGson
     */
    public static void setGson(Gson pGson) {
        GSON = pGson;
    }
}
