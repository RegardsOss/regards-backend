/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;

/**
 *
 * Class AccountSettingsController
 *
 * REST Controller to manage access global settings. Accesses are the state of project users during the activation
 * process
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping(path = AccessSettingsController.REQUEST_MAPPING_ROOT)
public class AccessSettingsController implements IResourceController<AccessSettings> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/accesses/settings";

    /**
     * Service handling CRUD operation on {@link AccessSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessSettingsService accessSettingsService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve the {@link AccountSettings}.
     *
     * @return The {@link AccountSettings}
     * @throws EntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the settings managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<AccessSettings>> retrieveAccessSettings() throws EntityNotFoundException {
        final AccessSettings accessSettings = accessSettingsService.retrieve();
        final Resource<AccessSettings> resource = new Resource<>(accessSettings);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Update the {@link AccountSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccountSettings}
     * @return The updated access settings
     * @throws EntityNotFoundException
     *             if no entity found!
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Updates the setting managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody final AccessSettings pAccessSettings)
            throws EntityNotFoundException {
        accessSettingsService.update(pAccessSettings);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<AccessSettings> toResource(final AccessSettings pElement, final Object... pExtras) {
        final Resource<AccessSettings> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveAccessSettings", LinkRels.SELF);
        resourceService.addLink(resource, this.getClass(), "updateAccessSettings", LinkRels.UPDATE,
                                MethodParamFactory.build(AccessSettings.class));

        return resource;
    }

}
