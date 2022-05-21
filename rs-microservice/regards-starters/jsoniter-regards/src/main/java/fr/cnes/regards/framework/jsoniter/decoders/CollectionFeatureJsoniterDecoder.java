package fr.cnes.regards.framework.jsoniter.decoders;

import com.google.gson.Gson;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.jsoniter.property.AttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.CollectionFeature;

import java.io.IOException;

public class CollectionFeatureJsoniterDecoder implements EntityFeatureDecoder, NullSafeDecoderBuilder {

    private final Gson gson;

    private final AttributeModelPropertyTypeFinder propTypeFinder;

    public CollectionFeatureJsoniterDecoder(Gson gson, AttributeModelPropertyTypeFinder propTypeFinder) {
        this.gson = gson;
        this.propTypeFinder = propTypeFinder;
    }

    public AttributeModelPropertyTypeFinder getPropTypeFinder() {
        return propTypeFinder;
    }

    @Override
    public Gson getGson() {
        return gson;
    }

    public static Decoder selfRegister(Gson gson, AttributeModelPropertyTypeFinder propTypeFinder) {
        Decoder decoder = new CollectionFeatureJsoniterDecoder(gson, propTypeFinder).nullSafe();
        JsoniterSpi.registerTypeDecoder(CollectionFeature.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any feature = iter.readAny();
        CollectionFeature result = new CollectionFeature(feature.as(UniformResourceName.class, "id"),
                                                         feature.toString("providerId"),
                                                         feature.toString("label"));
        result.setModel(feature.toString("model"));

        readBasicFields(feature, result);
        readGeometries(feature, result);
        readTagsFilesProperties(feature, result);

        return result;
    }

}
