/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.autoconfigure;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import fr.cnes.regards.framework.gson.adapters.PathAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;
import fr.cnes.regards.framework.gson.reflection.GsonAnnotationProcessor;
import fr.cnes.regards.framework.gson.strategy.GSonIgnoreExclusionStrategy;

/**
 * GSON support auto configuration
 *
 * @author Marc Sordi
 *
 */
@Configuration
@EnableConfigurationProperties(GsonProperties.class)
public class GsonAutoConfiguration implements ApplicationContextAware {

    @Autowired
    private GsonProperties properties;

    /**
     * Spring application context
     */
    private ApplicationContext applicationContext;

    @Bean
    public GsonBuilder gsonBuilder() {
        final GsonBuilder builder = new GsonBuilder();
        customizeBuilder(builder);
        addFactories(builder);
        addBeanFactories(builder);
        return builder;
    }

    @Bean
    public GsonHttpMessageConverter gsonConverter(GsonBuilder pBuilder) {
        final Gson gson = pBuilder.create();

        final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(gson);

        return gsonHttpMessageConverter;
    }

    private void customizeBuilder(GsonBuilder pBuilder) {
        pBuilder.registerTypeAdapter(Path.class, new PathAdapter().nullSafe());
        pBuilder.setExclusionStrategies(new GSonIgnoreExclusionStrategy());
    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactory}
     *
     * @param pBuilder
     *            GSON builder to customize
     */
    private void addFactories(GsonBuilder pBuilder) {
        GsonAnnotationProcessor.process(pBuilder, properties.getScanPrefix());
    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactoryBean} with Spring support.
     *
     * @param pBuilder
     *            GSON builder to customize
     */
    private void addBeanFactories(GsonBuilder pBuilder) {

        Map<String, TypeAdapterFactory> beanFactories = applicationContext.getBeansOfType(TypeAdapterFactory.class);
        if (beanFactories != null) {
            for (Map.Entry<String, TypeAdapterFactory> beanFactory : beanFactories.entrySet()) {
                pBuilder.registerTypeAdapterFactory(beanFactory.getValue());
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext pApplicationContext) {
        this.applicationContext = pApplicationContext;
    }

}
