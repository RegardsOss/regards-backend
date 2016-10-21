/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.web;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * Class MicroserviceAutoConfigure
 *
 * Auto configuration for microservices web mvc
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication
public class MicroserviceAutoConfigure {

    @Bean
    public WebMvcConfigurerAdapter configure() {
        return new MicroserviceWebConfiguration();
    }

}
