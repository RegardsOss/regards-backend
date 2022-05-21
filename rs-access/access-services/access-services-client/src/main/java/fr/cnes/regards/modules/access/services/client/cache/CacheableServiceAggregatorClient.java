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

import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Wrapper to IServiceAggregatorClient (Fiegn client) to add a cache.
 *
 * @author Binda Sébastien
 */
public class CacheableServiceAggregatorClient {

    private IServiceAggregatorClient serviceAggregatorClient;

    public CacheableServiceAggregatorClient(IServiceAggregatorClient serviceAggregatorClient) {
        this.serviceAggregatorClient = serviceAggregatorClient;
    }

    /**
     * Returns all services applied to all datasets plus those of the given dataset
     *
     * @param datasetIpId      the id of the Dataset
     * @param applicationModes the set of {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     */
    @Cacheable(value = ServiceAggregatorKeyGenerator.CACHE_NAME,
        keyGenerator = ServiceAggregatorKeyGenerator.KEY_GENERATOR, sync = true)
    public ResponseEntity<List<EntityModel<PluginServiceDto>>> retrieveServices(List<String> datasetIpId,
                                                                                List<ServiceScope> applicationModes) {
        return serviceAggregatorClient.retrieveServices(datasetIpId, applicationModes);
    }

}
