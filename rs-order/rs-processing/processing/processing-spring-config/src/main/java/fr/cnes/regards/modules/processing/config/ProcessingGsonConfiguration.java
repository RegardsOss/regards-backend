package fr.cnes.regards.modules.processing.config;

import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.gson.GsonProperties;
import fr.cnes.regards.modules.processing.utils.gson.TypedGsonTypeAdapter;
import io.vavr.gson.VavrGson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.ServiceLoader;

@Configuration
public class ProcessingGsonConfiguration {

    @Autowired private GsonProperties properties;
    @Autowired private ApplicationContext applicationContext;

    @Bean
    public GsonBuilderFactory gsonBuilderFactory() {
        return new GsonBuilderFactory(properties, applicationContext){
            @Override public GsonBuilder newBuilder() {
                GsonBuilder builder = GsonCustomizer.gsonBuilder(
                        Optional.ofNullable(properties),
                        Optional.ofNullable(applicationContext)
                );
                ServiceLoader<TypedGsonTypeAdapter> loader = ServiceLoader.load(TypedGsonTypeAdapter.class);
                loader.iterator().forEachRemaining(tr -> {
                    builder.registerTypeAdapter(tr.type(), tr.serializer());
                    builder.registerTypeAdapter(tr.type(), tr.deserializer());
                });
                VavrGson.registerAll(builder);
                VavrGson.registerAll(builder);
                return builder;
            }
        };
    }

}
