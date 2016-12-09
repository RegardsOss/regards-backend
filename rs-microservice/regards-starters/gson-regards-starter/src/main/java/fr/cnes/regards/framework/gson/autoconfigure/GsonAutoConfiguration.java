/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.autoconfigure;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

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
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
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
    public GsonWebConfiguration webConfig(GsonBuilder pBuilder) {
        return new GsonWebConfiguration(pBuilder);
    }

    private void addFactories(GsonBuilder pBuilder) {
        GsonAnnotationProcessor.process(pBuilder, properties.getScanPrefix());
    }

    private void customizeBuilder(GsonBuilder pBuilder) {
        pBuilder.registerTypeAdapter(Path.class, new PathAdapter().nullSafe());
        pBuilder.setExclusionStrategies(new GSonIgnoreExclusionStrategy());
    }
}
