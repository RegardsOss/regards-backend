/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.gson;

import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import fr.cnes.regards.framework.gson.adapters.*;
import fr.cnes.regards.framework.gson.adapters.actuator.ApplicationMappingsAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.BeanDescriptorAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.HealthAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.SystemHealthAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;
import fr.cnes.regards.framework.gson.strategy.GsonIgnoreExclusionStrategy;
import fr.cnes.regards.framework.gson.strategy.PagedModelExclusionStrategy;
import io.vavr.gson.VavrGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeanDescriptor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Static Gson customizer
 *
 * @author Marc Sordi
 */
public final class GsonCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonCustomizer.class);

    private GsonCustomizer() {
        // Nothing to do
    }

    public static GsonBuilder gsonBuilder(Optional<GsonProperties> properties,
                                          Optional<ApplicationContext> applicationContext) {
        GsonBuilder builder = new GsonBuilder().enableComplexMapKeySerialization();
        VavrGson.registerAll(builder);
        customizeBuilder(builder);
        addTypeAdapters(builder, properties);
        addBeanFactories(builder, applicationContext);
        addBeanAdapters(builder, applicationContext);
        return builder;
    }

    private static void customizeBuilder(GsonBuilder builder) {
        builder.registerTypeHierarchyAdapter(Path.class, new PathAdapter().nullSafe());
        builder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe());
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe());
        builder.registerTypeAdapter(MimeType.class, new MimeTypeAdapter().nullSafe());
        builder.registerTypeAdapter(Optional.class, new OptionalAdapter<>());
        builder.registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter().nullSafe());
        builder.registerTypeHierarchyAdapter(Multimap.class, new MultimapAdapter());
        builder.registerTypeHierarchyAdapter(MultiValueMap.class, new MultiValueMapAdapter());
        builder.setExclusionStrategies(new GsonIgnoreExclusionStrategy(), new PagedModelExclusionStrategy());

        // Custom actuator deserialization
        builder.registerTypeAdapter(Health.class, new HealthAdapter());
        builder.registerTypeAdapter(SystemHealth.class, new SystemHealthAdapter());
        builder.registerTypeAdapter(BeanDescriptor.class, new BeanDescriptorAdapter());
        builder.registerTypeAdapter(MappingsEndpoint.ApplicationMappingsDescriptor.class,
                                    new ApplicationMappingsAdapter());

        // Custom adapters for java collections
        builder.registerTypeAdapterFactory(ListAdapter.FACTORY);
        builder.registerTypeAdapterFactory(SetAdapter.FACTORY);
    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactory} and {@link TypeAdapter} annotated
     * with {@link GsonTypeAdapter}
     *
     * @param builder    GSON builder to customize
     * @param properties optional Gson properties
     */
    private static void addTypeAdapters(GsonBuilder builder, Optional<GsonProperties> properties) {
        if (properties.isPresent()) {
            GsonAnnotationProcessor.process(builder, properties.get().getScanPrefix());
            if (properties.get().getPrettyPrint()) {
                builder.setPrettyPrinting();
            }
            if (properties.get().getSerializeNulls()) {
                builder.serializeNulls();
            }
        }
    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactoryBean} with Spring support.
     *
     * @param builder            GSON builder to customize
     * @param applicationContext optional application context
     */
    private static void addBeanFactories(GsonBuilder builder, Optional<ApplicationContext> applicationContext) {

        if (applicationContext.isPresent()) {
            Map<String, TypeAdapterFactory> beanFactories = applicationContext.get()
                                                                              .getBeansOfType(TypeAdapterFactory.class);
            for (Map.Entry<String, TypeAdapterFactory> beanFactory : beanFactories.entrySet()) {
                builder.registerTypeAdapterFactory(beanFactory.getValue());
            }
        }
    }

    /**
     * Add {@link TypeAdapter}  and {@link TypedGsonTypeAdapter} annotated with {@link GsonTypeAdapterBean} to GSON
     * @param builder            GSON builder to customize
     * @param applicationContextOpt optional application context
     */
    @SuppressWarnings("rawtypes")
    private static void addBeanAdapters(GsonBuilder builder, Optional<ApplicationContext> applicationContextOpt) {
        if (applicationContextOpt.isPresent()) {
            ApplicationContext applicationContext = applicationContextOpt.get();
            // Search all classes inheriting TypeAdapter
            Map<String, TypeAdapter> typeAdapterMap = applicationContext.getBeansOfType(TypeAdapter.class);
            for (Map.Entry<String, TypeAdapter> entry : typeAdapterMap.entrySet()) {
                TypeAdapter<?> current = entry.getValue();
                // Retrieve custom annotation
                GsonTypeAdapterBean annot = current.getClass().getAnnotation(GsonTypeAdapterBean.class);
                if (annot != null) {
                    builder.registerTypeAdapter(annot.adapted(), current);
                } else {
                    LOGGER.debug("No annotation GsonTypeAdapterBean found on type adapter bean {}, skipping "
                                 + "registration", entry.getKey());
                }
            }

            // Search all classes implementing TypedGsonTypeAdapter

            Map<String, TypedGsonTypeAdapter> typedGsonTypeAdapterMap = applicationContext.getBeansOfType(
                TypedGsonTypeAdapter.class);
            for (Map.Entry<String, TypedGsonTypeAdapter> entry : typedGsonTypeAdapterMap.entrySet()) {
                TypedGsonTypeAdapter<?> current = entry.getValue();
                // Retrieve custom annotation
                GsonTypeAdapterBean annot = current.getClass().getAnnotation(GsonTypeAdapterBean.class);
                if (annot != null) {
                    builder.registerTypeAdapter(annot.adapted(), current.serializer());
                    builder.registerTypeAdapter(annot.adapted(), current.deserializer());
                } else {
                    LOGGER.debug("No annotation GsonTypeAdapterBean found on type adapter bean {}, skipping "
                                 + "registration", entry.getKey());
                }
            }

        }
    }
}
