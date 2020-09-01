package fr.cnes.regards.modules.processing.testutils;

import com.google.gson.Gson;
import feign.Request;
import feign.RequestTemplate;
import feign.codec.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class GsonLoggingEncoder implements Encoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonLoggingEncoder.class);

    private final Gson gson;

    public GsonLoggingEncoder(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        String bodyText = gson.toJson(object, bodyType);
        LOGGER.info("Encoding object {}\n>>>\n{}\n<<<", object, bodyText);
        template.body(bodyText);
    }

}
