/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.web;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.microservices.core.manage.ApplicationManager;

/**
 *
 * Class MicroserviceAutoConfigure
 *
 * Auto configuration for microservices web mvc
 *
 * @author CS
 * @author svissier
 * @since 1.0-SNAPSHOT
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication
public class MicroserviceAutoConfigure {

    @Bean
    public ApplicationManager applicationManager(final ApplicationContext pApplicationContext) {
        return new ApplicationManager(pApplicationContext);
    }

    /**
     *
     * Allow to configure specific web MVC properties for incoming and out-going requests.
     *
     * @return MicroserviceWebConfiguration
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public MicroserviceWebConfiguration webConfig() {
        return new MicroserviceWebConfiguration();
    }

}
