/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;

/**
 * Class AccountSettingsController
 *
 * REST Controller to manage access global settings. Accesses are the state of project users during the activation
 * process
 * @author Sébastien Binda
 */
@RestController
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
     * Retrieve the {@link AccessSettings}.
     * @return The {@link AccessSettings}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the settings managing the access requests",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<AccessSettings>> retrieveAccessSettings() {
        AccessSettings accessSettings = accessSettingsService.retrieve();
        return new ResponseEntity<>(toResource(accessSettings), HttpStatus.OK);
    }

    /**
     * Update the {@link AccessSettings}.
     * @param accessSettings The {@link AccessSettings}
     * @return The updated access settings
     * @throws EntityNotFoundException if no entity found!
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Updates the setting managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<AccessSettings>> updateAccessSettings(
            @Valid @RequestBody AccessSettings accessSettings) throws EntityNotFoundException {
        accessSettings = accessSettingsService.update(accessSettings);
        return new ResponseEntity<>(toResource(accessSettings), HttpStatus.OK);
    }

    @Override
    public EntityModel<AccessSettings> toResource(final AccessSettings element, final Object... extras) {
        EntityModel<AccessSettings> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAccessSettings", LinkRels.SELF);
        resourceService.addLink(resource, this.getClass(), "updateAccessSettings", LinkRels.UPDATE,
                                MethodParamFactory.build(AccessSettings.class));

        return resource;
    }

}
