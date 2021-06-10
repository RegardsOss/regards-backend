package fr.cnes.regards.modules.indexer.dao.deser;

import com.google.gson.Gson;
import fr.cnes.regards.modules.indexer.domain.IIndexable;

public class GsonDeserializeIIndexableStrategy implements JsonDeserializeStrategy<IIndexable> {

    private final Gson gson;

    public GsonDeserializeIIndexableStrategy(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <U extends IIndexable> U deserializeJson(String sourceAsString, Class<U> clazz) {
        return gson.fromJson(sourceAsString, clazz);
    }
}
