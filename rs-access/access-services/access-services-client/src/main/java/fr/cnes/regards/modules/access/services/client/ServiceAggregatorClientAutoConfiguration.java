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
package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Autoconfiguration class for {@link IServiceAggregatorClient}
 *
 * @author Xavier-Alexandre Brochard
 */
//@Configuration
//@EnableCaching
//@AutoConfigureAfter({ FeignAutoConfiguration.class, FeignClientConfiguration.class,
//        FeignSecurityAutoConfiguration.class, FeignAutoConfiguration.class })
public class ServiceAggregatorClientAutoConfiguration {

    /**
     * Handle events on plugin links and configuration for managing caching and cache evictions
     * @param subscriber
     * @param runtimeTenantResolver
     * @param serviceAggregatorClient
     * @return {@link ServiceAggregatorClientEventHandler}
     */
    //    @Bean
    ServiceAggregatorClientEventHandler serviceAggregatorClientEventHandler(ISubscriber subscriber,
            IRuntimeTenantResolver runtimeTenantResolver, IServiceAggregatorClient serviceAggregatorClient) {
        return new ServiceAggregatorClientEventHandler(subscriber, runtimeTenantResolver, serviceAggregatorClient);
    }
}
