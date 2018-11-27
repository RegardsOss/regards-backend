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
package fr.cnes.regards.framework.microservice.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import fr.cnes.regards.framework.microservice.configurer.MaintenanceWebSecurityConfiguration;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceHealthIndicator;
import fr.cnes.regards.framework.microservice.manager.DefaultApplicationManager;
import fr.cnes.regards.framework.microservice.manager.IApplicationManager;
import fr.cnes.regards.framework.microservice.web.MicroserviceWebConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;

/**
 * FIXME microservice starter should not depends on security starter to manage maintenance mode!
 *
 * Class MicroserviceAutoConfigure
 *
 * Auto configuration for microservices web mvc
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
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
    @ConditionalOnProperty(prefix = "regards.microservices", name = "maintenance.enabled", havingValue = "true",
            matchIfMissing = true)
    public MaintenanceHealthIndicator maintenanceHealthIndicator(IRuntimeTenantResolver runtimeTenantResolver) {
        return new MaintenanceHealthIndicator(runtimeTenantResolver);
    }

    @Bean
    @ConditionalOnProperty(prefix = "regards.microservices", name = "maintenance.enabled", havingValue = "true",
            matchIfMissing = true)
    public ICustomWebSecurityConfiguration maintenanceWebSecurity(IRuntimeTenantResolver runtimeTenantResolver) {
        return new MaintenanceWebSecurityConfiguration(runtimeTenantResolver);
    }
}
