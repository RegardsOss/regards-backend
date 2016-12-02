/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.autoconfigure;

import java.nio.file.Path;
import java.util.Set;

import org.reflections.Reflections;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.Gsonable;
import fr.cnes.regards.framework.microservice.manager.DefaultApplicationManager;
import fr.cnes.regards.framework.microservice.manager.IApplicationManager;
import fr.cnes.regards.framework.microservice.web.MicroserviceWebConfiguration;
import fr.cnes.regards.framework.microservice.web.PathAdapter;

/**
 *
 * Class MicroserviceAutoConfigure
 *
 * Auto configuration for microservices web mvc
 *
 * @author CS
 * @author svissier
 * @since 1.0-SNAPSHOT
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@ConditionalOnWebApplication
public class MicroserviceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IApplicationManager applicationManager() {
        return new DefaultApplicationManager();
    }

    @Bean
    public GsonBuilder gsonBuilder() {
        final GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Path.class, new PathAdapter().nullSafe());
        addGsonablePojos(builder);
        return builder;
    }

    /**
     * method introspecting sub packages of fr.cnes.regards to detect entity annotated by {@link Gsonable}.
     *
     * @param pBuilder
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchFieldException
     */
    @SuppressWarnings("unchecked")
    private void addGsonablePojos(GsonBuilder pBuilder) {
        final Reflections reflections = new Reflections("fr.cnes.regards");
        final Set<Class<?>> gsonables = reflections.getTypesAnnotatedWith(Gsonable.class);
        for (Class<?> gsonable : gsonables) {
            final Set<?> subTypes = reflections.getSubTypesOf(gsonable);
            if (!subTypes.isEmpty()) {
                final String discriminatorFieldName = gsonable.getAnnotation(Gsonable.class).value();
                final PolymorphicTypeAdapterFactory<?> typeAdapterFactory = PolymorphicTypeAdapterFactory
                        .of(gsonable, discriminatorFieldName);
                for (Object subType : subTypes) {
                    final Class subTypeClass = (Class) subType;
                    if (discriminatorFieldName.isEmpty()) {
                        typeAdapterFactory.registerSubtype(subTypeClass);
                    } else {
                        // TODO: to make the reflection fully generic and functionnal. ISSUE: how to get the value of
                        // enums via reflection
                        // final Field discriminatorField = gsonable.getField(discriminatorFieldName);
                        // final Class<?> discriminatorFieldType = discriminatorField.getType();
                        // if (discriminatorFieldType.isEnum()) {
                        // typeAdapterFactory.registerSubtype(subTypeClass, Enum
                        // .valueOf((Class<Enum>) discriminatorFieldType, discriminatorField
                        // .get(discriminatorFieldType.getEnumConstants()[0]).toString()));
                        // } else {
                        // if (discriminatorFieldType.isAssignableFrom(String.class)) {
                        // typeAdapterFactory.registerSubtype(subTypeClass, (String) discriminatorField.get(""));
                        // }
                        // }
                    }
                }
                // TODO: find a way to specify a type adapter that will be used to customize the serialization of a
                // specific type
                pBuilder.registerTypeHierarchyAdapter(gsonable,
                                                      typeAdapterFactory.create(new Gson(), TypeToken.get(gsonable)));
            }
        }
    }

    /**
     *
     * Allow to configure specific web MVC properties for incoming and out-going requests.
     *
     * @return MicroserviceWebConfiguration
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public MicroserviceWebConfiguration webConfig(GsonBuilder pBuilder) {
        return new MicroserviceWebConfiguration(pBuilder);
    }

}
