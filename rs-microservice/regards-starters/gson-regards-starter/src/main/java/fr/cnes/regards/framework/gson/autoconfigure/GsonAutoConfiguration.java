/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.autoconfigure;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.gson.adapters.PathAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;
import fr.cnes.regards.framework.gson.reflection.GsonAnnotationProcessor;
import fr.cnes.regards.framework.gson.strategy.GsonIgnoreExclusionStrategy;

/**
 * GSON support auto configuration
 *
 * @author Marc Sordi
 */
@Configuration
@EnableConfigurationProperties(GsonProperties.class)
@AutoConfigureBefore({ HttpMessageConvertersAutoConfiguration.class })
public class GsonAutoConfiguration implements ApplicationContextAware {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GsonAutoConfiguration.class);

    /**
     * Factory to adapter swagger documentation serialization
     */
    private static final String SPRINGFOX_GSON_FACTORY = "fr.cnes.regards.framework.swagger.gson.SpringFoxTypeFactory";

    @Autowired
    private GsonProperties properties;

    /**
     * Spring application context
     */
    private ApplicationContext applicationContext;

    public GsonBuilder gsonBuilder() {
        final GsonBuilder builder = new GsonBuilder();
        customizeBuilder(builder);
        addTypeAdapters(builder);
        addBeanFactories(builder);
        addBeanAdapters(builder);
        return builder;
    }

    /**
     * Configure a builder with GSON adapter for Sprinfox swagger Json object
     *
     * @return {@link GsonBuilder}
     */
    @Bean
    @ConditionalOnClass(name = SPRINGFOX_GSON_FACTORY)
    public GsonBuilder configureWithSwagger() {
        LOGGER.info("GSON auto configuration enabled with SpringFox support");
        GsonBuilder builder = gsonBuilder();
        try {
            builder.registerTypeAdapterFactory((TypeAdapterFactory) Class.forName(SPRINGFOX_GSON_FACTORY)
                    .newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            final String errorMessage = "Cannot init SpringFox GSON factory";
            LOGGER.error(errorMessage, e);
            throw new UnsupportedOperationException(errorMessage);
        }
        return builder;
    }

    @Bean
    @ConditionalOnMissingClass(SPRINGFOX_GSON_FACTORY)
    public GsonBuilder configure() {
        LOGGER.info("GSON auto configuration enabled");
        return gsonBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public Gson gson(GsonBuilder pBuilder) {
        return pBuilder.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public GsonHttpMessageConverter gsonConverter(Gson pGson) {
        final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(pGson);

        return gsonHttpMessageConverter;
    }

    private void customizeBuilder(GsonBuilder pBuilder) {
        pBuilder.registerTypeAdapter(Path.class, new PathAdapter().nullSafe());
        pBuilder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe());
        pBuilder.addSerializationExclusionStrategy(new GsonIgnoreExclusionStrategy());

    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactory} and {@link TypeAdapter} annotated
     * with {@link GsonTypeAdapter}
     *
     * @param pBuilder
     *            GSON builder to customize
     */
    private void addTypeAdapters(GsonBuilder pBuilder) {
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

    /**
     * Add {@link TypeAdapter} annotated with {@link GsonTypeAdapterBean} to GSON
     *
     * @param pBuilder
     *            GSON builder to customize
     */
    private void addBeanAdapters(GsonBuilder pBuilder) {

        @SuppressWarnings("rawtypes")
        Map<String, TypeAdapter> beanFactories = applicationContext.getBeansOfType(TypeAdapter.class);
        if (beanFactories != null) {
            for (@SuppressWarnings("rawtypes")
            Map.Entry<String, TypeAdapter> beanFactory : beanFactories.entrySet()) {
                TypeAdapter<?> current = beanFactory.getValue();
                // Retrieve custom annotation
                GsonTypeAdapterBean annot = current.getClass().getAnnotation(GsonTypeAdapterBean.class);
                if (annot != null) {
                    pBuilder.registerTypeAdapter(annot.type(), beanFactory.getValue());
                } else {
                    LOGGER.debug("No annotation found on type adapter bean {}, skipping registration",
                                 beanFactory.getKey());
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext pApplicationContext) {
        applicationContext = pApplicationContext;
    }
}
