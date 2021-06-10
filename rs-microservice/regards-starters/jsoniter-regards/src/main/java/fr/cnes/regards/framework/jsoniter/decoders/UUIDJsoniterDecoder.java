package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;

import java.io.IOException;
import java.util.UUID;

public class UUIDJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new UUIDJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(UUID.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        return UUID.fromString(iter.readString());
    }

}

