/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.cloud.gateway.cors;

import java.util.Arrays;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration class for gateway CORS policy
 * @author Marc Sordi
 */
@Configuration
@EnableConfigurationProperties(CorsConfigurationProperties.class)
public class DefaultCorsConfiguration {

    /**
     * Enable cors for Zuul proxy
     * @return {@link CorsFilter} configuration
     */
    @Bean
    public CorsFilter corsFilter(CorsConfigurationProperties corsConfigurationProperties) {

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        corsConfigurationProperties.getMappings().forEach((path, corsProperties) -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(corsProperties.isAllowCredentials());
            config.setAllowedOrigins(Arrays.asList(corsProperties.getAllowedOrigins()));
            config.setAllowedHeaders(Arrays.asList(corsProperties.getAllowedHeaders()));
            config.setAllowedMethods(Arrays.asList(corsProperties.getAllowedMethods()));
            config.setMaxAge(corsProperties.getMaxAge());
            source.registerCorsConfiguration(path, config);
        });
        return new CorsFilter(source);
    }
}
