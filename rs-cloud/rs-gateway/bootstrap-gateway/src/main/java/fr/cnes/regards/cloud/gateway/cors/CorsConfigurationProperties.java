/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Properties to configure gateway CORS policy
 *
 * @author Marc Sordi
 *
 */
@ConfigurationProperties(prefix = "regards.gateway.cors")
public class CorsConfigurationProperties {

    private Map<String, CorsProperties> mappings = new HashMap<>();

    public static class CorsProperties {

        @NotNull
        private String[] allowedOrigins;

        private String[] allowedHeaders = { "authorization", "content-type", "scope" };

        private String[] allowedMethods = { "OPTIONS", "HEAD", "GET", "PUT", "POST", "DELETE", "PATCH" };

        private long maxAge = 3600;

        private boolean allowCredentials = true;

        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String[] pAllowedOrigins) {
            allowedOrigins = pAllowedOrigins;
        }

        public String[] getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String[] pAllowedHeaders) {
            allowedHeaders = pAllowedHeaders;
        }

        public String[] getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String[] pAllowedMethods) {
            allowedMethods = pAllowedMethods;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long pMaxAge) {
            maxAge = pMaxAge;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean pAllowCredentials) {
            allowCredentials = pAllowCredentials;
        }
    }

    public Map<String, CorsProperties> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, CorsProperties> pMappings) {
        mappings = pMappings;
    }

}

// @Bean
// public CorsFilter corsFilter() {
// final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
// final CorsConfiguration config = new CorsConfiguration();
// config.setAllowCredentials(true);
// config.addAllowedOrigin("*");
// config.addAllowedHeader("authorization");
// config.addAllowedHeader("content-type");
// config.addAllowedHeader("scope");
// config.addAllowedMethod("OPTIONS");
// config.addAllowedMethod("HEAD");
// config.addAllowedMethod("GET");
// config.addAllowedMethod("PUT");
// config.addAllowedMethod("POST");
// config.addAllowedMethod("DELETE");
// config.addAllowedMethod("PATCH");
// config.setMaxAge(3600L);
// source.registerCorsConfiguration("/api/**", config);
// return new CorsFilter(source);
// }
