package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import org.springframework.util.MimeType;

import java.io.IOException;

public class MimeTypeJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new MimeTypeJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(MimeType.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        return MimeType.valueOf(iter.readString());
    }
}
