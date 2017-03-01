/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.feign.FeignSecurityManager;

/**
 * Feign auto configuration
 *
 * @author Marc Sordi
 *
 */
@Configuration
public class FeignAutoConfiguration {

    @Bean
    public FeignSecurityManager securityManager() {
        return new FeignSecurityManager();
    }
}
