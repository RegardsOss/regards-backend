package fr.cnes.regards.framework.jsoniter.autoconfigure;

import com.google.gson.Gson;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.jsoniter.IIndexableJsoniterConfig;
import fr.cnes.regards.framework.jsoniter.JsoniterDecoderRegisterer;
import fr.cnes.regards.framework.jsoniter.property.AttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.jsoniter.property.JsoniterAttributeModelPropertyTypeFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jsoniter support auto configuration
 */
@Configuration
public class JsoniterAutoConfiguration {

    @Bean
    public AttributeModelPropertyTypeFinder propTypeFinder() {
        return new JsoniterAttributeModelPropertyTypeFinder();
    }

    @Bean
    public IIndexableJsoniterConfig indexableJsoniterConfig() {
        return new IIndexableJsoniterConfig();
    }

    @Bean
    public JsoniterDecoderRegisterer jsoniterDecoderRegisterer(AttributeModelPropertyTypeFinder propTypeFinder,
                                                               IIndexableJsoniterConfig spiConfig,
                                                               Gson gson) {
        JsoniterSpi.setCurrentConfig(spiConfig);
        return new JsoniterDecoderRegisterer(propTypeFinder, gson);
    }

}
