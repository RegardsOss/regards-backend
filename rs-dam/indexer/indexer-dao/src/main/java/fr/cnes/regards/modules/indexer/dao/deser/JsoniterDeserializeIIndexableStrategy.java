package fr.cnes.regards.modules.indexer.dao.deser;

import com.jsoniter.JsonIterator;
import fr.cnes.regards.framework.jsoniter.IIndexableJsoniterConfig;
import fr.cnes.regards.modules.indexer.domain.IIndexable;

public class JsoniterDeserializeIIndexableStrategy implements JsonDeserializeStrategy<IIndexable> {

    private final IIndexableJsoniterConfig config;

    public JsoniterDeserializeIIndexableStrategy(IIndexableJsoniterConfig config) {
        this.config = config;
    }

    @Override
    public <U extends IIndexable> U deserializeJson(String sourceAsString, Class<U> clazz) {
        return JsonIterator.deserialize(config, sourceAsString, clazz);
    }
}
