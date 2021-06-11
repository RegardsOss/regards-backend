package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;

import java.io.IOException;
import java.net.URL;

public class URLJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new URLJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(URL.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        return new URL(iter.readString());
    }

}

