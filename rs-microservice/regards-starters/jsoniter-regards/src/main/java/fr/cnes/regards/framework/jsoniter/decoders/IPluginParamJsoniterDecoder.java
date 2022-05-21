package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;

import java.io.IOException;

public class IPluginParamJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new IPluginParamJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(IPluginParam.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        try {
            Any param = iter.readAny();
            String type = param.toString("@type@");
            return param.as(Class.forName(type));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
