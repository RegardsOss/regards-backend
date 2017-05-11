/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.domain.ServiceScope;
import fr.cnes.regards.modules.search.service.IServiceManager;

/**
 * REST Controller handling operations on services.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
@RequestMapping(ServicesController.PATH_SERVICES)
public class ServicesController {

    public static final String PATH_SERVICES = "/services/{dataset_id}";

    public static final String PATH_SERVICE_NAME = "/{service_name}";

    @Autowired
    private IServiceManager serviceManager;

    /**
     * Retrieve all services configured for a dataset and a given scope
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}
     * @param pServiceScope
     *            the {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(
            description = "endpoint allowing to retrieve all services configured for a dataset and a given scope")
    public ResponseEntity<Set<PluginConfiguration>> retrieveServices(
            @PathVariable("dataset_id") final String pDatasetId,
            @RequestParam("service_scope") final ServiceScope pServiceScope) throws EntityNotFoundException {
        final Set<PluginConfiguration> services = serviceManager.retrieveServices(pDatasetId, pServiceScope);
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    /**
     * Apply the given service.
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}
     * @param pServiceName
     *            the {@link PluginConfiguration}'s label to be executed
     * @param pQuery
     *            the query to be interpreted to get the objects on which apply the service
     * @param pQueryParameters
     *            the query parameters
     * @return whatever is returned by the given service
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = PATH_SERVICE_NAME)
    @ResponseBody
    @ResourceAccess(
            description = "endpoint allowing to apply the given service on objects retrieved thanks to the given query")
    public ResponseEntity<?> applyService(@PathVariable("dataset_id") final String pDatasetId,
            @PathVariable("service_name") final String pServiceName,
            @RequestParam final Map<String, String> pQueryParameters) throws ModuleException {
        return serviceManager.apply(pDatasetId, pServiceName, pQueryParameters);
    }

}
