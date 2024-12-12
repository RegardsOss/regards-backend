package fr.cnes.regards.framework.jackson.autoconfigure;

import fr.cnes.regards.framework.geojson.CustomJacksonAutoconfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Autoconfiguration to deserialize/serialize http requests/responses with Jackson
 *
 * @author Thibaud Michaudel
 */
@AutoConfiguration(after = CustomJacksonAutoconfiguration.class)
public class JacksonConverterAutoConfiguration {

    final Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;

    public JacksonConverterAutoConfiguration(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        this.jackson2ObjectMapperBuilder = jackson2ObjectMapperBuilder;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "regards.http.converter", havingValue = "jackson")
    public AbstractJackson2HttpMessageConverter jacksonConverter() {
        return new JacksonHttpMessageConverterCustom(jackson2ObjectMapperBuilder.build());
    }
}
