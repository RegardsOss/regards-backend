/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.security.endpoint.DefaultAuthorityProvider;
import fr.cnes.regards.framework.security.endpoint.DefaultPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.endpoint.IPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.event.SecurityEventHandler;

/**
 * Method Authorization Service auto configuration
 *
 * @author msordi
 *
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureBefore(MultitenantAutoConfiguration.class)
public class MethodAuthorizationServiceAutoConfiguration {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.instance.tenant.name:instance}")
    private String instanceTenantName;

    @ConditionalOnMissingBean
    @Bean
    public IRuntimeTenantResolver secureThreadTenantResolver() {
        return new SecureRuntimeTenantResolver(instanceTenantName);
    }

    @Bean
    @ConditionalOnMissingBean
    public IAuthoritiesProvider authoritiesProvider() {
        return new DefaultAuthorityProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodAuthorizationService methodAuthorizationService() {
        return new MethodAuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public IPluginResourceManager pluginResourceManager() {
        return new DefaultPluginResourceManager();
    }

    @Bean
    public SecurityEventHandler securityEventHandler(final ISubscriber subscriber) {
        return new SecurityEventHandler(microserviceName, subscriber, methodAuthorizationService());
    }
}
