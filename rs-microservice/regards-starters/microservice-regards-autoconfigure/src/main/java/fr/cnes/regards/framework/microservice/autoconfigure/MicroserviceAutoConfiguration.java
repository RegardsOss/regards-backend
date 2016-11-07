/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.microservice.manager.DefaultApplicationManager;
import fr.cnes.regards.framework.microservice.manager.IApplicationManager;
import fr.cnes.regards.framework.microservice.web.MicroserviceWebConfiguration;

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
public class MicroserviceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IApplicationManager applicationManager() {
        return new DefaultApplicationManager();
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
