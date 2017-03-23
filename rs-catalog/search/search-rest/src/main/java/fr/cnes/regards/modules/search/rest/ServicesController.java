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
import fr.cnes.regards.modules.search.domain.ServiceScope;
import fr.cnes.regards.modules.search.service.IServiceManager;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@RequestMapping(ServicesController.PATH_SERVICES)
public class ServicesController {

    public static final String PATH_SERVICES = "/services/{dataset_id}";

    public static final String PATH_SERVICE_NAME = "/{service_name}";

    @Autowired
    private IServiceManager serviceManager;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(
            description = "endpoint allowing to retrieve all services configured for a dataset and a given scope")
    public ResponseEntity<Set<PluginConfiguration>> retrieveServices(@PathVariable("dataset_id") Long pDatasetId,
            @RequestParam("scope") ServiceScope pServiceScope) throws EntityNotFoundException {
        Set<PluginConfiguration> services = serviceManager.retrieveServices(pDatasetId, pServiceScope);
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    /**
     *
     * Apply the given service.
     *
     * @param pServiceName
     *            the {@link PluginConfiguration}'s label to be executed
     * @param pQuery
     *            the query to be interpreted to get the objects on which apply the service
     * @param pQueryParameters
     * @return
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = PATH_SERVICE_NAME)
    @ResponseBody
    @ResourceAccess(
            description = "endpoint allowing to apply the given service on objects retrieved thanks to the given query")
    public ResponseEntity<?> applyService(@PathVariable("dataset_id") Long pDatasetId,
            @PathVariable("service_name") String pServiceName, @RequestParam Map<String, String> pQueryParameters)
            throws ModuleException {
        return serviceManager.apply(pDatasetId, pServiceName, pQueryParameters);
    }

}
