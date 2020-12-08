package fr.cnes.regards.modules.feature.dto.gson;

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FeatureProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProperties.class);

    public static final String PROPERTIES_FIELD_NAME = "properties";

    private FeatureProperties() {}

    public static void beforeRead(JsonObject wrapper) {
        JsonElement attEl = wrapper.get(PROPERTIES_FIELD_NAME);
        if ((attEl != null) && !attEl.isJsonNull()) {
            if (attEl.isJsonObject()) {
                wrapper.add(PROPERTIES_FIELD_NAME, restoreArray(attEl.getAsJsonObject()));
            } else {
                throw objectRequiredException(attEl);
            }
        }
    }

    /**
     * Restore {@link JsonArray} from flattened {@link JsonObject} elements (reverse merge)
     * @param jsonObject {@link JsonObject} to transform
     * @return {@link JsonArray}
     */
    private static JsonArray restoreArray(JsonObject jsonObject) {
        JsonArray restoredArray = new JsonArray();
        for (Map.Entry<String, JsonElement> nestedEntry : jsonObject.entrySet()) {
            JsonObject nestedObject = new JsonObject();
            nestedObject.add(nestedEntry.getKey(), nestedEntry.getValue());
            restoredArray.add(nestedObject);
        }
        return restoredArray;
    }

    public static void beforeWrite(JsonObject wrapper) {
        JsonElement attEl = wrapper.get(PROPERTIES_FIELD_NAME);
        if ((attEl != null) && !attEl.isJsonNull()) {
            if (attEl.isJsonArray()) {
                wrapper.add(PROPERTIES_FIELD_NAME, mergeArray(attEl.getAsJsonArray()));
            } else {
                String errorMessage = String.format("Unexpected JSON element %s. Array required.", attEl.toString());
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

    /**
     * Merge {@link JsonArray} flattening elements in a single {@link JsonObject}
     * @param jsonArray {@link JsonArray} to flatten
     * @return {@link JsonObject}
     */
    private static JsonObject mergeArray(JsonArray jsonArray) {
        JsonObject mergedObject = new JsonObject();
        Iterator<JsonElement> nestedIter = jsonArray.iterator();
        while (nestedIter.hasNext()) {
            JsonElement nested = nestedIter.next();
            if (nested.isJsonObject()) {
                JsonObject nestedObject = nested.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : nestedObject.entrySet()) {
                    mergedObject.add(e.getKey(), e.getValue());
                }
            } else {
                throw objectRequiredException(nested);
            }
        }
        return mergedObject;
    }

    public static IllegalArgumentException objectRequiredException(JsonElement jsonElement) {
        String errorMessage = String.format("Unexpected JSON element %s. Object required.", jsonElement.toString());
        LOGGER.error(errorMessage);
        return new IllegalArgumentException(errorMessage);
    }
}
