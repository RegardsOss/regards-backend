/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;

/**
 *
 * Autoconfigure feign security without profile restriction
 * 
 * @author Marc Sordi
 *
 */
@Configuration
public class FeignSecurityAutoConfiguration {

    @Bean
    public FeignSecurityManager feignSecurityManager() {
        return new FeignSecurityManager();
    }
}
