/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import fr.cnes.regards.modules.configuration.service.IUILayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping("/layouts")
public class UILayoutController implements IResourceController<UILayout> {

    @Autowired
    private IUILayoutService UILayoutService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a {@link UILayout}
     *
     * @return {@link UILayout}
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM UILayout configuration for the given applicationId",
                    role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<UILayout>> retrieveUILayout(@PathVariable("applicationId") final String applicationId)
        throws EntityNotFoundException {
        final UILayout UILayout = UILayoutService.retrieveLayout(applicationId);
        final EntityModel<UILayout> resource = toResource(UILayout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to update {@link UILayout}
     *
     * @return updated {@link UILayout}
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM UILayout configuration for the given applicationId",
                    role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<UILayout>> updateUILayout(@PathVariable("applicationId") final String applicationId,
                                                            @Valid @RequestBody final UILayout UILayout)
        throws EntityException {
        final UILayout updated = UILayoutService.updateLayout(UILayout);
        final EntityModel<UILayout> resource = toResource(updated);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public EntityModel<UILayout> toResource(final UILayout element, final Object... extras) {
        final EntityModel<UILayout> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveUILayout",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getApplicationId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateUILayout",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, element.getApplicationId()),
                                MethodParamFactory.build(UILayout.class));
        return resource;
    }

}
