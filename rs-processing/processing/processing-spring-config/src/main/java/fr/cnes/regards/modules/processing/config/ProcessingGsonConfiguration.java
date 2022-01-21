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

/**
 * This class is the configuration for Gson.
 *
 * @author gandrieu
 */
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
                return builder;
            }
        };
    }

}
