/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.search.service.ServiceManager;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@RequestMapping(ServicesController.PATH_SERVICES)
public class ServicesController implements IResourceController<DataObject> {

    public static final String PATH_SERVICES = "/services";

    public static final String PATH_SERVICE_NAME = "/{service_name}";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private ServiceManager serviceManager;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "endpoint allowing to retrieve all services configured")
    public ResponseEntity<List<PluginConfiguration>> retrieveServices() {
        List<PluginConfiguration> services = serviceManager.retrieveServices();
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
    public ResponseEntity<List<Resource<DataObject>>> applyService(@PathVariable("service_name") String pServiceName,
            @RequestParam("q") String pQuery, @RequestParam Map<String, String> pQueryParameters)
            throws ModuleException {
        // query parameters contains the query too, lets remove it so we only have plugin parameter
        pQueryParameters.remove("q");
        Set<DataObject> results = serviceManager.apply(pServiceName, pQueryParameters, pQuery);
        return new ResponseEntity<>(toResources(results), HttpStatus.OK);
    }

    @Override
    public Resource<DataObject> toResource(DataObject pElement, Object... pExtras) {
        Resource<DataObject> resource = resourceService.toResource(pElement);
        // TODO Auto-generated method stub
        return resource;
    }

}
