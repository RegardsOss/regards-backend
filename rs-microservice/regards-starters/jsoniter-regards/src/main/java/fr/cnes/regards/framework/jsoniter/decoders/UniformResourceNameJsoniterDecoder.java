package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.urn.UniformResourceName;

import java.io.IOException;

public class UniformResourceNameJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new UniformResourceNameJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(UniformResourceName.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        return UniformResourceName.fromString(iter.readString());
    }

}
