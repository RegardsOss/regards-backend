/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.services.client;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * Feign client for calling ServicesAggregatorController methods
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-access")
@RequestMapping(value = "/services/aggregated", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@FunctionalInterface
public interface IServiceAggregatorClient {

    /**
     * Returns all services applied to all datasets plus those of the given dataset
     *
     * @param pDatasetIpId
     *            the id of the {@link Dataset}
     * @param pApplicationModes
     *            the set of {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     * @throws EntityNotFoundException
     */
    @Cacheable(value = "servicesAggregated")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Resource<PluginServiceDto>>> retrieveServices(
            @RequestParam(value = "datasetIpId", required = false) final String pDatasetIpId,
            @RequestParam(value = "applicationMode", required = false) final ServiceScope pApplicationMode);
}