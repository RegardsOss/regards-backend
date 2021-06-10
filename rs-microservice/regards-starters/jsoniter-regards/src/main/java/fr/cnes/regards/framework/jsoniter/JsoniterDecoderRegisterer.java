package fr.cnes.regards.framework.jsoniter;

import com.google.gson.Gson;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.jsoniter.decoders.*;
import fr.cnes.regards.framework.jsoniter.property.AttributeModelPropertyTypeFinder;
import io.vavr.collection.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class JsoniterDecoderRegisterer {

    private final List<Decoder> decoders;

    public JsoniterDecoderRegisterer(AttributeModelPropertyTypeFinder propTypeFinder, Gson gson) {
        this.decoders = registerDecoders(propTypeFinder, gson);
    }

    private static List<Decoder> registerDecoders(AttributeModelPropertyTypeFinder propTypeFinder, Gson gson) {
        JsoniterSpi.registerTypeImplementation(ConcurrentMap.class, ConcurrentHashMap.class);

        return List.of(
                MimeTypeJsoniterDecoder.selfRegister(),
                DataObjectFeatureJsoniterDecoder.selfRegister(gson, propTypeFinder),
                CollectionFeatureJsoniterDecoder.selfRegister(gson, propTypeFinder),
                DatasetFeatureJsoniterDecoder.selfRegister(gson, propTypeFinder),
                DataObjectMetadataJsoniterDecoder.selfRegister(),
                DataObjectGroupJsoniterDecoder.selfRegister(),
                DataObjectJsoniterDecoder.selfRegister(),
                DatasetJsoniterDecoder.selfRegister(),
                CollectionJsoniterDecoder.selfRegister(),
                IGeometryJsoniterDecoder.selfRegister(),
                ICriterionJsoniterDecoder.selfRegister(),
                IPluginParamJsoniterDecoder.selfRegister(),
                UUIDJsoniterDecoder.selfRegister(),
                URLJsoniterDecoder.selfRegister(),
                OffsetDateTimeJsoniterDecoder.selfRegister(),
                UniformResourceNameJsoniterDecoder.selfRegister(),
                IIndexableJsoniterDecoder.selfRegister()
        );
    }

}
