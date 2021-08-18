package fr.cnes.regards.framework.jsoniter.decoders;

import com.google.gson.Gson;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.jsoniter.property.AttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;

import java.io.IOException;

public class DatasetFeatureJsoniterDecoder implements EntityFeatureDecoder, NullSafeDecoderBuilder {

    private final Gson gson;
    private final AttributeModelPropertyTypeFinder propTypeFinder;

    public DatasetFeatureJsoniterDecoder(Gson gson, AttributeModelPropertyTypeFinder propTypeFinder) {
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
        Decoder decoder = new DatasetFeatureJsoniterDecoder(gson, propTypeFinder).nullSafe();
        JsoniterSpi.registerTypeDecoder(DatasetFeature.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any feature = iter.readAny();
        DatasetFeature result = new DatasetFeature(
            UniformResourceName.fromString(feature.toString("id")),
            feature.toString("providerId"),
            feature.toString("label"),
            feature.toString("licence")
        );

        readBasicFields(feature, result);
        readGeometries(feature, result);
        readTagsFilesProperties(feature, result);

        return result;
    }

}
