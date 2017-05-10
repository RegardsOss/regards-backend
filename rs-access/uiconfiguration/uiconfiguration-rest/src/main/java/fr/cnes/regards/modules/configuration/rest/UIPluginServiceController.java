/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;
import fr.cnes.regards.modules.configuration.service.IUIPluginConfigurationService;

/**
 *
 * Class UIPluginServiceController
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "PluginServices", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(UIPluginServiceController.REQUEST_MAPPING_ROOT)
public class UIPluginServiceController implements IResourceController<UIPluginConfiguration> {

    public static final String REQUEST_MAPPING_ROOT = "/services/{dataset_id}";

    /**
     * Business service to manage {@link UIPluginConfiguration} entities
     */
    @Autowired
    private IUIPluginConfigurationService service;

    /**
     * Service to manage hateoas resources
     */
    @Autowired
    private IResourceService resourceService;

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
    @ResourceAccess(role = DefaultRole.PUBLIC,
            description = "endpoint allowing to retrieve all services configured for a dataset and a given scope")
    public ResponseEntity<List<Resource<UIPluginConfiguration>>> retrieveServices(
            @PathVariable("dataset_id") final String pDatasetId) throws EntityNotFoundException {
        final List<UIPluginConfiguration> services = service.retrieveActivePluginServices(pDatasetId);
        return new ResponseEntity<>(toResources(services), HttpStatus.OK);
    }

    @Override
    public Resource<UIPluginConfiguration> toResource(final UIPluginConfiguration pElement, final Object... pExtras) {
        return resourceService.toResource(pElement);
    }

}
