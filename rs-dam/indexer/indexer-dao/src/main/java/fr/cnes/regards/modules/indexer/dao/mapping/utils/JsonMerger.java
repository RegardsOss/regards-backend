package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Arrays;

public class JsonMerger {

    public JsonObject merge(JsonObject one, JsonObject two) {
        JsonObject merged = new JsonObject();
        one.entrySet().forEach(entry -> {
            String key = entry.getKey();
            JsonElement oneValue = entry.getValue();
            if (two.has(key)) {
                JsonElement twoValue = two.get(key);
                if (twoValue.equals(oneValue)) {
                    merged.add(key, oneValue);
                } else if (oneValue.isJsonObject() && twoValue.isJsonObject()) {
                    merged.add(key, merge(oneValue.getAsJsonObject(), twoValue.getAsJsonObject()));
                } else if (oneValue.isJsonArray() && twoValue.isJsonArray()) {
                    merged.add(key, merge(oneValue.getAsJsonArray(), twoValue.getAsJsonArray()));
                } else {
                    merged.add(key, twoValue);
                }
            } else {
                merged.add(key, oneValue);
            }
        });
        two.entrySet().stream().filter(entry -> !one.has(entry.getKey()))
                .forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
        return merged;
    }

    public JsonArray merge(JsonArray one, JsonArray two) {
        if (one.equals(two)) {
            return one;
        }
        JsonArray merged = new JsonArray();
        one.iterator().forEachRemaining(merged::add);
        two.iterator().forEachRemaining(el -> {
            if (!merged.contains(el)) {
                merged.add(el);
            }
        });
        return merged;
    }

    public JsonObject mergeAll(JsonObject... objs) {
        return Arrays.stream(objs).reduce(new JsonObject(), this::merge, this::merge);
    }
}
