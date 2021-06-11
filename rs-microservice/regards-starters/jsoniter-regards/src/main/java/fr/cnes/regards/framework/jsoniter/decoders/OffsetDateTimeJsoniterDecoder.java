package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;

import java.io.IOException;
import java.time.OffsetDateTime;

public class OffsetDateTimeJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new OffsetDateTimeJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(OffsetDateTime.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        return OffsetDateTimeAdapter.parse(iter.readString());
    }

}

