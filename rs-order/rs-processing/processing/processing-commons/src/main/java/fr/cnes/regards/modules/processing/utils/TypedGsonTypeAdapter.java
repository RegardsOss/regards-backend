package fr.cnes.regards.modules.processing.utils;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;

public interface TypedGsonTypeAdapter<T> {

    Class<T> type();
    JsonDeserializer<T> deserializer();
    JsonSerializer<T> serializer();

}
