package fr.cnes.regards.modules.processing.utils;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.adapters.*;
import fr.cnes.regards.framework.gson.adapters.actuator.ApplicationMappingsAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.BeanDescriptorAdapter;
import fr.cnes.regards.framework.gson.adapters.actuator.HealthAdapter;
import fr.cnes.regards.framework.gson.strategy.GsonIgnoreExclusionStrategy;
import io.vavr.gson.VavrGson;
import org.jeasy.random.api.Randomizer;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ServiceLoader;

public class ProcessingGsonUtils {

    public static Gson gsonPretty() {
        GsonBuilder builder = gsonBuilder();
        return builder.setPrettyPrinting().create();
    }
    public static Gson gson() {
        GsonBuilder builder = gsonBuilder();
        return builder.create();
    }

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
        builder.registerTypeAdapter(MappingsEndpoint.ApplicationMappings.class, new ApplicationMappingsAdapter());
        VavrGson.registerAll(builder);

        ServiceLoader<TypedGsonTypeAdapter> loader = ServiceLoader.load(TypedGsonTypeAdapter.class);
        loader.iterator().forEachRemaining(tr -> {
            builder.registerTypeAdapter(tr.type(), tr.serializer());
            builder.registerTypeAdapter(tr.type(), tr.deserializer());
        });

        return builder;
    }

}
