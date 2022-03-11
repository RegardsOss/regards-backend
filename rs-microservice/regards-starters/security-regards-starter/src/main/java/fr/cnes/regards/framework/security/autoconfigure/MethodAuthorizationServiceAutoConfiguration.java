/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.authentication.autoconfigure.AuthenticationAutoConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.test.SecureTestRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.*;
import fr.cnes.regards.framework.security.event.SecurityEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Method Authorization Service auto configuration
 * @author msordi
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureBefore({ AuthenticationAutoConfiguration.class, MultitenantAutoConfiguration.class })
public class MethodAuthorizationServiceAutoConfiguration {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.instance.tenant.name:instance}")
    private String instanceTenantName;

    @Autowired
    Environment env;

    @ConditionalOnMissingBean
    @Profile("!test")
    @Bean
    public IRuntimeTenantResolver secureThreadTenantResolver() {
        return new SecureRuntimeTenantResolver(instanceTenantName);
    }

    @ConditionalOnMissingBean
    @Profile("test")
    @Bean
    public IRuntimeTenantResolver secureTestThreadTenantResolver() {
        return new SecureTestRuntimeTenantResolver(instanceTenantName);
    }

    @ConditionalOnMissingBean
    @Bean
    public IAuthenticationResolver secureThreadAuthenticationResolver() {
        return new SecureRuntimeAuthenticationResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public IAuthoritiesProvider authoritiesProvider() {
        return new DefaultAuthorityProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regards.microservice.type", havingValue = "multitenant", matchIfMissing = true)
    public MethodAuthorizationService methodAuthorizationService() {
        return new MethodAuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regards.microservice.type", havingValue = "instance", matchIfMissing = false)
    public MethodAuthorizationService instanceMethodAuthorizationService() {
        return new InstanceMethodAuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public IPluginResourceManager pluginResourceManager() {
        return new DefaultPluginResourceManager();
    }

    @Bean
    public SecurityEventHandler securityEventHandler(final ISubscriber subscriber,
            MethodAuthorizationService methodAuthorizationService) {
        return new SecurityEventHandler(microserviceName, subscriber, methodAuthorizationService);
    }
}
