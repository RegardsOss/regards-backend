package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DatasetMetadata.DataObjectGroup;

import java.io.IOException;

public class DataObjectGroupJsoniterDecoder implements NullSafeDecoderBuilder {


    public static Decoder selfRegister() {
        Decoder decoder = new DataObjectGroupJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(DataObjectGroup.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any group = iter.readAny();
        return new DataObjectGroup(
            group.toString("groupName"),
            group.toBoolean("datasetAccess"),
            group.toBoolean("dataObjectAccess"),
            group.toLong("metaDataObjectAccessFilterPluginId"),
            group.toLong("dataObjectAccessFilterPluginId")
        );
    }
}
