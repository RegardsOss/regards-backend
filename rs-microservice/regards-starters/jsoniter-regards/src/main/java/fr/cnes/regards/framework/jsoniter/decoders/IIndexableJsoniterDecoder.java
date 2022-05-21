package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.indexer.domain.IIndexable;

import java.io.IOException;

public class IIndexableJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new IIndexableJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(IIndexable.class, decoder);
        JsoniterSpi.registerTypeDecoder(AbstractEntity.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any indexable = iter.readAny();
        EntityType type = indexable.get("type").as(EntityType.class);
        switch (type) {
            case COLLECTION:
                return indexable.as(Collection.class);
            case DATASET:
                return indexable.as(Dataset.class);
            case DATA:
            default:
                return indexable.as(DataObject.class);
        }
    }
}
