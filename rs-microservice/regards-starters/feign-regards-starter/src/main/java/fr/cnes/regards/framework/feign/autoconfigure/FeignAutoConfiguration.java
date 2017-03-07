/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;

/**
 * Feign auto configuration
 *
 * @author Marc Sordi
 *
 */
@Configuration
@EnableFeignClients("fr.cnes.regards")
@AutoConfigureAfter(name = "fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration")
public class FeignAutoConfiguration {

    @Bean
    public FeignSecurityManager feignSecurityManager() {
        return new FeignSecurityManager();
    }

}
