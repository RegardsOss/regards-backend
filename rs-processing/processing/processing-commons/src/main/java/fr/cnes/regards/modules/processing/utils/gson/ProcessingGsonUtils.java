/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils.gson;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.adapters.*;
import fr.cnes.regards.framework.gson.adapters.actuator.ApplicationMappingsAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.BeanDescriptorAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.HealthAdapter;
import fr.cnes.regards.framework.gson.strategy.GsonIgnoreExclusionStrategy;
import io.vavr.gson.VavrGson;
import org.reflections.Reflections;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Gson builder with type adapters.
 *
 * @author gandrieu
 */
public class ProcessingGsonUtils {

    private ProcessingGsonUtils() {
    }

    public static Gson gsonPretty() {
        GsonBuilder builder = gsonBuilder();
        return builder.setPrettyPrinting().create();
    }

    public static Gson gson() {
        GsonBuilder builder = gsonBuilder();
        return builder.create();
    }

    @SuppressWarnings("rawtypes")
    public static GsonBuilder gsonBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(Path.class, new PathAdapter().nullSafe());
        builder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe());
        builder.registerTypeAdapter(Duration.class, new GsonDurationAdapter().nullSafe());

        builder.registerTypeAdapter(MimeType.class, new MimeTypeAdapter().nullSafe());
        builder.registerTypeHierarchyAdapter(Multimap.class, new MultimapAdapter());
        builder.registerTypeHierarchyAdapter(MultiValueMap.class, new MultiValueMapAdapter());
        builder.addSerializationExclusionStrategy(new GsonIgnoreExclusionStrategy());
        builder.registerTypeAdapter(Health.class, new HealthAdapter());
        builder.registerTypeAdapter(BeansEndpoint.BeanDescriptor.class, new BeanDescriptorAdapter());
        builder.registerTypeAdapter(MappingsEndpoint.ApplicationMappingsDescriptor.class,
                                    new ApplicationMappingsAdapter());
        VavrGson.registerAll(builder);

        // Register gson type adapters implementing TypedGsonTypeAdapter. This is usualy done into GsonCustomizer but
        // here, there isn't Spring so do it manually.
        Reflections reflections = new Reflections("fr.cnes.regards");
        // Search for all classes implementing TypedGsonTypeAdapter
        for (Class<? extends TypedGsonTypeAdapter> clazz : reflections.getSubTypesOf(TypedGsonTypeAdapter.class)) {
            try {
                // build an instance (constructor without parameter wanted please)
                TypedGsonTypeAdapter<?> typedGsonTypeAdapter = clazz.getConstructor().newInstance();
                // Retrieve adapted type (i.e. class parameter, assuming only one interface is implemented)
                Type actualTypeArgument =
                    ((ParameterizedType) clazz.getGenericInterfaces()[0]).getActualTypeArguments()[0];
                // Register both JsonSerializer and JsonDeserializer
                builder.registerTypeAdapter(actualTypeArgument, typedGsonTypeAdapter.serializer());
                builder.registerTypeAdapter(actualTypeArgument, typedGsonTypeAdapter.deserializer());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

        }

        return builder;
    }

}
