/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.geojson.deserializers.GeometryDeserializerModule;
import fr.cnes.regards.framework.geojson.serializers.GeometrySerializerModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author Thomas GUILLOU
 **/
@Configuration
public class GeojsonJacksonAutoconfiguration {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        // Spring Boot actually uses this builder by default when building the ObjectMapper
        // it will also register automatically jackson-datatype-jdk8 modules because it's present on the classpath
        // jackson-datatype-jdk8: support for other Java 8 types like Optional
        return new Jackson2ObjectMapperBuilder().modulesToInstall(new GeometrySerializerModule(),
                                                                  new GeometryDeserializerModule())
                                                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
                                                .failOnUnknownProperties(false);
    }

}
