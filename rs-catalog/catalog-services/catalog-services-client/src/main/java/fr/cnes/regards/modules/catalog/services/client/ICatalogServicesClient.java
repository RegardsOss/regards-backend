/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.catalog.services.client;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;

/**
 * Feign client for calling rs-catalog's CatalogServicesController
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = "/services", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ICatalogServicesClient {

    /**
     * Call rs-catalog's CatalogServicesController#retrieveServices
     * @param datasetId the id of the Dataset. Can be <code>null</code>.
     * @param serviceScope the applicable mode. Can be <code>null</code>.
     * @return the list of services
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<PluginConfigurationDto>>> retrieveServices(
            @RequestParam(value = "dataset_ids", required = false) final List<String> datasetIds,
            @RequestParam(value = "service_scope", required = false) final ServiceScope serviceScope);

}