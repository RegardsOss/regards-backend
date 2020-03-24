/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeanDescriptor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint.ApplicationMappings;
import org.springframework.context.ApplicationContext;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

import fr.cnes.regards.framework.gson.adapters.MimeTypeAdapter;
import fr.cnes.regards.framework.gson.adapters.MultiValueMapAdapter;
import fr.cnes.regards.framework.gson.adapters.MultimapAdapter;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.gson.adapters.PathAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.ApplicationMappingsAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.BeanDescriptorAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.HealthAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;
import fr.cnes.regards.framework.gson.strategy.GsonIgnoreExclusionStrategy;

/**
 * Static Gson customizer
 * @author Marc Sordi
 */
public final class GsonCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonCustomizer.class);

    private GsonCustomizer() {
        // Nothing to do
    }

    public static GsonBuilder gsonBuilder(Optional<GsonProperties> properties,
            Optional<ApplicationContext> applicationContext) {
        GsonBuilder builder = new GsonBuilder();
        customizeBuilder(builder);
        addTypeAdapters(builder, properties);
        addBeanFactories(builder, applicationContext);
        addBeanAdapters(builder, applicationContext);
        return builder;
    }

    private static void customizeBuilder(GsonBuilder builder) {
        builder.registerTypeHierarchyAdapter(Path.class, new PathAdapter().nullSafe());
        builder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe());
        builder.registerTypeAdapter(MimeType.class, new MimeTypeAdapter().nullSafe());
        builder.registerTypeHierarchyAdapter(Multimap.class, new MultimapAdapter());
        builder.registerTypeHierarchyAdapter(MultiValueMap.class, new MultiValueMapAdapter());
        builder.addSerializationExclusionStrategy(new GsonIgnoreExclusionStrategy());
        // Custom actuator deserialization
        builder.registerTypeAdapter(Health.class, new HealthAdapter());
        builder.registerTypeAdapter(BeanDescriptor.class, new BeanDescriptorAdapter());
        builder.registerTypeAdapter(ApplicationMappings.class, new ApplicationMappingsAdapter());
    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactory} and {@link TypeAdapter} annotated
     * with {@link GsonTypeAdapter}
     * @param builder GSON builder to customize
     * @param properties optional Gson properties
     */
    private static void addTypeAdapters(GsonBuilder builder, Optional<GsonProperties> properties) {
        if (properties.isPresent()) {
            GsonAnnotationProcessor.process(builder, properties.get().getScanPrefix());
        }
    }

    /**
     * Add {@link TypeAdapterFactory} annotated with {@link GsonTypeAdapterFactoryBean} with Spring support.
     * @param builder GSON builder to customize
     * @param applicationContext optional application context
     */
    private static void addBeanFactories(GsonBuilder builder, Optional<ApplicationContext> applicationContext) {

        if (applicationContext.isPresent()) {
            Map<String, TypeAdapterFactory> beanFactories = applicationContext.get()
                    .getBeansOfType(TypeAdapterFactory.class);
            if (beanFactories != null) {
                for (Map.Entry<String, TypeAdapterFactory> beanFactory : beanFactories.entrySet()) {
                    builder.registerTypeAdapterFactory(beanFactory.getValue());
                }
            }
        }
    }

    /**
     * Add {@link TypeAdapter} annotated with {@link GsonTypeAdapterBean} to GSON
     * @param builder GSON builder to customize
     * @param applicationContext optional application context
     */
    private static void addBeanAdapters(GsonBuilder builder, Optional<ApplicationContext> applicationContext) {

        if (applicationContext.isPresent()) {
            @SuppressWarnings("rawtypes")
            Map<String, TypeAdapter> beanFactories = applicationContext.get().getBeansOfType(TypeAdapter.class);
            if (beanFactories != null) {
                for (@SuppressWarnings("rawtypes")
                Map.Entry<String, TypeAdapter> beanFactory : beanFactories.entrySet()) {
                    TypeAdapter<?> current = beanFactory.getValue();
                    // Retrieve custom annotation
                    GsonTypeAdapterBean annot = current.getClass().getAnnotation(GsonTypeAdapterBean.class);
                    if (annot != null) {
                        builder.registerTypeAdapter(annot.adapted(), beanFactory.getValue());
                    } else {
                        LOGGER.debug("No annotation found on type adapter bean {}, skipping registration",
                                     beanFactory.getKey());
                    }
                }
            }
        }
    }
}
