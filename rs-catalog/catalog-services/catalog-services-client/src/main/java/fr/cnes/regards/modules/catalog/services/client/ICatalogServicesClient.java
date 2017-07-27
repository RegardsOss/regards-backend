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
 * Feign client for calling rs-catalog's {@link CatalogServicesController}
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = "/services", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ICatalogServicesClient {

    /**
     * Call rs-catalog's {@link CatalogServicesController#retrieveServices}
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}. Can be <code>null</code>.
     * @param pServiceScope
     *            the applicable mode. Can be <code>null</code>.
     * @return the list of services
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Resource<PluginConfigurationDto>>> retrieveServices(
            @RequestParam(value = "dataset_id", required = false) final String pDatasetId,
            @RequestParam(value = "service_scope", required = false) final ServiceScope pServiceScope);

}