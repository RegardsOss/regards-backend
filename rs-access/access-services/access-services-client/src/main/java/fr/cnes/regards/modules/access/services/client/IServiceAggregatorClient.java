/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

/**
 * Feign client for calling ServicesAggregatorController methods
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-access-project", contextId = "rs-access-project.service-agg-client")
@RequestMapping(value = "/services/aggregated", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IServiceAggregatorClient {

    /**
     * Logger
     */
    Logger LOGGER = LoggerFactory.getLogger(IServiceAggregatorClient.class);

    String CACHE_NAME = "servicesAggregated";

    /**
     * Returns all services applied to all datasets plus those of the given dataset
     * @param datasetIpId the id of the Dataset
     * @param applicationMode
     * @param pApplicationModes the set of {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     */
    @Cacheable(value = IServiceAggregatorClient.CACHE_NAME, sync = true)
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<PluginServiceDto>>> retrieveServices(
            @RequestParam(value = "datasetIpIds", required = false) final List<String> datasetIpId,
            @RequestParam(value = "applicationModes", required = false) final List<ServiceScope> applicationMode);

    /**
     * Empty the whole "servicesAggregated" cache. Maybe we can perform a finer eviction?
     * <p>Note that it is not annotated with @RequestMapping, as it does not refer to a real endpoint
     * We placed this method here because of
     * @see 'https://stackoverflow.com/questions/10343885/spring-3-1-cacheable-method-still-executed/10347208#10347208'
     */
    @CacheEvict(cacheNames = IServiceAggregatorClient.CACHE_NAME, allEntries = true)
    default void clearServicesAggregatedCache() {
        LOGGER.debug("Rejecting all entries of servicesAggregated cache");
    }
}
