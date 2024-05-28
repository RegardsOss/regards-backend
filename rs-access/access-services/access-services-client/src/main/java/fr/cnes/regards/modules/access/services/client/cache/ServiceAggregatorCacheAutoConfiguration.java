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
package fr.cnes.regards.modules.access.services.client.cache;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * SPRING Cache autoconfiguration class
 *
 * @author Sébastien Binda
 */
@Profile("!test")
@AutoConfiguration
@EnableCaching
public class ServiceAggregatorCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean(ServiceAggregatorKeyGenerator.KEY_GENERATOR)
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public IServiceAggregatorKeyGenerator serviceAggregatorKeyGenerator(IAuthenticationResolver oauthResolver,
                                                                        IRuntimeTenantResolver resolver) {
        return new ServiceAggregatorKeyGenerator(oauthResolver, resolver);
    }

    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public ServiceAggregatorClientEventHandler serviceAggregatorEventHandler(ISubscriber subscriber,
                                                                             IServiceAggregatorKeyGenerator keyGen) {
        return new ServiceAggregatorClientEventHandler(subscriber, keyGen);
    }

    @Bean
    public CacheableServiceAggregatorClient cacheableServiceAggregatorClient(IServiceAggregatorClient serviceAggregatorClient) {
        return new CacheableServiceAggregatorClient(serviceAggregatorClient);
    }

}
