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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import fr.cnes.regards.framework.microservice.configurer.MaintenanceConfiguration;
import fr.cnes.regards.framework.microservice.manager.DefaultApplicationManager;
import fr.cnes.regards.framework.microservice.manager.IApplicationManager;
import fr.cnes.regards.framework.microservice.web.MicroserviceWebConfiguration;

/**
 *
 * Class MicroserviceAutoConfigure
 *
 * Auto configuration for microservices web mvc
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 *
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class MicroserviceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IApplicationManager applicationManager() {
        return new DefaultApplicationManager();
    }

    @Bean
    public MicroserviceWebConfiguration webConfig() {
        return new MicroserviceWebConfiguration();
    }
    
    @Bean
    public MaintenanceConfiguration maintenanceConf() {
        return new MaintenanceConfiguration();
    }

}
