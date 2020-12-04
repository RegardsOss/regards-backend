package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.*;

public class GsonBetter {

    private GsonBetter() {}

    public static JsonPrimitive str(String str) { return new JsonPrimitive(str); }
    public static JsonPrimitive num(int i) { return new JsonPrimitive(i); }
    public static JsonPrimitive num(double d) { return new JsonPrimitive(d); }
    public static JsonPrimitive bool(boolean b) { return new JsonPrimitive(b); }

    public static JsonArray array(JsonElement... elements) {
        JsonArray arr = new JsonArray();
        for (JsonElement el : elements) {
            arr.add(el);
        }
        return arr;
    }

    public static class KV {
        String key;
        JsonElement value;
        public KV(String key, JsonElement value) {
            this.key = key;
            this.value = value;
        }
    }
    public static KV kv(String key, JsonElement value) { return new KV(key, value); }
    public static KV kv(String key, String value) { return new KV(key, str(value)); }
    public static KV kv(String key, int value) { return new KV(key, num(value)); }
    public static KV kv(String key, double value) { return new KV(key, num(value)); }
    public static KV kv(String key, boolean value) { return new KV(key, bool(value)); }


    public static JsonObject object(KV... keyValues) {
        JsonObject obj = new JsonObject();
        for (KV kv : keyValues) {
            obj.add(kv.key, kv.value);
        }
        return obj;
    }
    // Shortcuts when there is only one KV pair
    public static JsonObject object(String key, JsonElement value) { return object(kv(key, value)); }
    public static JsonObject object(String key, String value) { return object(kv(key, value)); }
    public static JsonObject object(String key, int value) { return object(kv(key, value)); }
    public static JsonObject object(String key, double value) { return object(kv(key, value)); }
    public static JsonObject object(String key, boolean value) { return object(kv(key, value)); }

}
