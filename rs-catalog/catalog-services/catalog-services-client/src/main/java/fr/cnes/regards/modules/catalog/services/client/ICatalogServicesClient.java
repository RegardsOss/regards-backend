/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.catalog.services.client;

import java.util.Collection;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;

/**
 * Feign client for calling rs-catalog's {@link CatalogServicesController}
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = "/services/{dataset_id}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ICatalogServicesClient {

    /**
     * Call rs-catalog's {@link CatalogServicesController#retrieveServicesWithMeta}
     *
     * @param pDatasetId
     * @return whatever the controller returns
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, path = "/meta")
    public ResponseEntity<Collection<Resource<PluginConfigurationDto>>> retrieveServicesWithMeta(
            @PathVariable("dataset_id") final String pDatasetId);

}