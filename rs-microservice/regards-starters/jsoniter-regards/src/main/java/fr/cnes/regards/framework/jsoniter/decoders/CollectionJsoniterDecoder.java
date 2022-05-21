package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.feature.CollectionFeature;
import fr.cnes.regards.modules.model.domain.Model;

import java.io.IOException;

public class CollectionJsoniterDecoder implements AbstractEntityDecoder<CollectionFeature, Collection> {

    public static Decoder selfRegister() {
        Decoder decoder = new CollectionJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(Collection.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any collection = iter.readAny();
        CollectionFeature feature = collection.as(CollectionFeature.class, "feature");

        Collection result = new Collection(collection.as(Model.class, "model"), feature);

        readCommonFields(collection, feature, result);

        return result;
    }

}
