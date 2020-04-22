/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * SPRING Cache autoconfiguration class
 *
 * @author Sébastien Binda
 *
 */
@Profile("!test")
@Configuration
@EnableCaching
public class ServiceAggregatorCacheAutoConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean(ServiceAggregatorKeyGenerator.KEY_GENERATOR)
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    IServiceAggregatorKeyGenerator serviceAggregatorKeyGenerator(IAuthenticationResolver oauthResolver,
            IRuntimeTenantResolver resolver) {
        return new ServiceAggregatorKeyGenerator(oauthResolver, resolver);
    }

    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ServiceAggregatorClientEventHandler serviceAggregatorEventHandler(ISubscriber subscriber,
            IServiceAggregatorKeyGenerator rolesKeyGen) {
        return new ServiceAggregatorClientEventHandler(subscriber, rolesKeyGen);
    }

}
