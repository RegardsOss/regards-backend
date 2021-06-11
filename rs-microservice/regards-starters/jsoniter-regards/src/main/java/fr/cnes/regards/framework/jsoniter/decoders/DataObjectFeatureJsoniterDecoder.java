package fr.cnes.regards.framework.jsoniter.decoders;

import com.google.gson.Gson;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.jsoniter.property.AttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;

import java.io.IOException;

public class DataObjectFeatureJsoniterDecoder implements EntityFeatureDecoder, NullSafeDecoderBuilder {

    private final Gson gson;
    private final AttributeModelPropertyTypeFinder propTypeFinder;

    public DataObjectFeatureJsoniterDecoder(Gson gson, AttributeModelPropertyTypeFinder propTypeFinder) {
        this.gson = gson;
        this.propTypeFinder = propTypeFinder;
    }

    public Gson getGson() {
        return gson;
    }

    public AttributeModelPropertyTypeFinder getPropTypeFinder() {
        return propTypeFinder;
    }

    public static Decoder selfRegister(Gson gson, AttributeModelPropertyTypeFinder propTypeFinder) {
        Decoder decoder = new DataObjectFeatureJsoniterDecoder(gson, propTypeFinder).nullSafe();
        JsoniterSpi.registerTypeDecoder(DataObjectFeature.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any feature = iter.readAny();
        DataObjectFeature result = new DataObjectFeature(
            UniformResourceName.fromString(feature.toString("id")),
            stringOrNull(feature, "providerId"),
            stringOrNull(feature, "label"),
            stringOrNull(feature, "sessionOwner"),
            stringOrNull(feature, "session"),
            stringOrNull(feature, "model")
        );

        readBasicFields(feature, result);
        readGeometries(feature, result);
        readTagsFilesProperties(feature, result);

        return result;
    }

}
