/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.autoconfigure;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.gson.adapters.PathAdapter;
import fr.cnes.regards.framework.gson.reflection.GsonAnnotationProcessor;
import fr.cnes.regards.framework.gson.strategy.GSonIgnoreExclusionStrategy;

/**
 * GSON support auto configuration
 *
 * @author Marc Sordi
 *
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(GsonProperties.class)
public class GsonAutoConfiguration {

    @Autowired
    private GsonProperties properties;

    @Bean
    public GsonBuilder gsonBuilder() {
        final GsonBuilder builder = new GsonBuilder();
        customizeBuilder(builder);
        addFactories(builder);
        return builder;
    }

    @Bean
    public GsonHttpMessageConverter gsonConverter(GsonBuilder pBuilder) {
        final Gson gson = pBuilder.create();

        final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(gson);

        return gsonHttpMessageConverter;
    }

    private void addFactories(GsonBuilder pBuilder) {
        GsonAnnotationProcessor.process(pBuilder, properties.getScanPrefix());
    }

    private void customizeBuilder(GsonBuilder pBuilder) {
        pBuilder.registerTypeAdapter(Path.class, new PathAdapter().nullSafe());
        pBuilder.setExclusionStrategies(new GSonIgnoreExclusionStrategy());
    }
}
