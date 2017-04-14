/*
 * LICENSE_PLACEHOLDER
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
 *
 * @author Marc Sordi
 *
 */
@Configuration
@EnableConfigurationProperties(CorsConfigurationProperties.class)
public class DefaultCorsConfiguration {

    /**
     * Enable cors for Zuul proxy
     *
     * @param custom
     *            CORS properties
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
