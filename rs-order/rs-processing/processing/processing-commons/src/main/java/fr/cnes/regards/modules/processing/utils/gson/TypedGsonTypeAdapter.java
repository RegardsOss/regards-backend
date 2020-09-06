package fr.cnes.regards.modules.processing.utils.gson;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

/**
 * This interface is meant to be used as a target for ServiceLoader, so that
 * other components can independently declare new type adapters to be loaded
 * by ProcessingGsonUtils.
 *
 * @param <T> the generic type
 */
public interface TypedGsonTypeAdapter<T> {

    Class<T> type();
    JsonDeserializer<T> deserializer();
    JsonSerializer<T> serializer();

}
