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
package fr.cnes.regards.microservices.administration;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.client.rest.ITenantClient;
import fr.cnes.regards.modules.project.client.rest.ITenantConnectionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Class MicroserviceTenantResolverAutoConfigure
 * <p>
 * Autoconfiguration class for Microservices multitenant resolver
 *
 * @author Sébastien Binda
 */
@AutoConfiguration
@Profile("production")
@EnableCaching
@AutoConfigureBefore({ DataSourcesAutoConfiguration.class, MultitenantJpaAutoConfiguration.class })
public class RemoteTenantAutoConfiguration {

    /**
     * Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * @param tenantConnectionClient Feign clien
     * @return {@link ITenantConnectionResolver}
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantConnectionResolver multitenantResolver(final DiscoveryClient discoveryClient,
                                                  final ITenantConnectionClient tenantConnectionClient) {
        return new RemoteTenantConnectionResolver(discoveryClient, tenantConnectionClient);
    }

    /**
     * Remote tenant resolver. Retrieve tenants from the administration service.
     *
     * @param pInitClients Do not use the IProjectsClient. It must not be initialized at this time. To do so, we use the specials
     *                     administration clients manually configured {@link FeignInitialAdminClients}
     * @return RemoteTenantResolver
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantResolver tenantResolver(final DiscoveryClient pDiscoveryClient, final ITenantClient tenantClient) {
        return new RemoteTenantResolver(pDiscoveryClient, tenantClient, microserviceName);
    }

    /**
     * Handle tenant and tenant connection events for managing cache evictions
     *
     * @param subscriber {@link ISubscriber}
     * @return {@link RemoteTenantEventHandler}
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    RemoteTenantEventHandler remoteTenantEventHandler(final IInstanceSubscriber subscriber,
                                                      RemoteTenantResolver remoteTenantResolver) {
        return new RemoteTenantEventHandler(subscriber, remoteTenantResolver);
    }
}
