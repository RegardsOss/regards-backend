package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectMetadata;

import java.io.IOException;

public class DataObjectMetadataJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new DataObjectMetadataJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(DataObjectMetadata.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        DataObjectMetadata result = new DataObjectMetadata();
        Any any = iter.readAny();
        any.get("groups").asMap().forEach((group, accessRights) -> {
            accessRights.forEach(accessRight -> {
                String dataset = accessRight.toString("dataset");
                boolean right = accessRight.toBoolean("dataAccessRight");
                result.addGroup(group, dataset, right);
            });
        });
        any.get("modelNames").asMap().forEach((modelName, ids) -> {
            ids.forEach(id -> result.addModelName(modelName, id.toString()));
        });
        return result;
    }
}
