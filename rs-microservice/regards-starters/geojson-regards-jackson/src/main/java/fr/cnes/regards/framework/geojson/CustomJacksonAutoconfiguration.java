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
package fr.cnes.regards.framework.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.cnes.regards.framework.geojson.modules.EntityModelSerializerModule;
import fr.cnes.regards.framework.geojson.modules.GeometrySerializerModule;
import fr.cnes.regards.framework.geojson.modules.MimeTypeSerializerModule;
import fr.cnes.regards.framework.geojson.modules.PageModelSerializerModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration bean to register modules in the default jackson ObjectMapper which is constructed when calling new ObjectMapper()
 * To add a custom module (to permits serialization of a non-generic class), create in your maven
 * module the file /META-INF/services/com.fasterxml.jackson.databind.Module containing the qualifier of your mobule so
 * it's actually used by jackson for serialization and deserialization.
 *
 * @author Thomas GUILLOU
 **/
@AutoConfiguration
public class CustomJacksonAutoconfiguration {

    /**
     * Create an ObjectMapperBuilder.
     * The default ObjectMapper configuration that is used when new ObjectMapper() is called will be overridden
     */
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        // Spring Boot actually uses this builder by default when building the ObjectMapper
        // it will also register automatically some modules because they are present on the classpath :
        // jackson-datatype-jdk8: support for other Java 8 types like Optional
        // jackson-datatype-jsr310: support for Java 8 Date & Time API types
        return new Jackson2ObjectMapperBuilder().modulesToInstall(new MimeTypeSerializerModule(),
                                                                  new GeometrySerializerModule(),
                                                                  new PageModelSerializerModule(),
                                                                  new EntityModelSerializerModule())
                                                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
                                                .failOnUnknownProperties(false)
                                                // Spring Cloud default config has disable WRITE_DATES_AS_TIMESTAMPS
                                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
