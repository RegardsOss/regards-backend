/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.service.ILayoutService;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Layout", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/layouts")
public class LayoutController implements IResourceController<Layout> {

    @Autowired
    private ILayoutService layoutService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a {@link Layout}
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM layout configuration for the given applicationId",
            role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Layout>> retrieveLayout(@PathVariable("applicationId") final String pApplicationId)
            throws EntityNotFoundException {
        final Layout layout = layoutService.retrieveLayout(pApplicationId);
        final Resource<Layout> resource = toResource(layout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to update {@link Layout}
     *
     * @return updated {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM layout configuration for the given applicationId",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Layout>> updateLayout(@PathVariable("applicationId") final String pApplicationId,
            @Valid @RequestBody final Layout pLayout) throws EntityException {
        final Layout layout = layoutService.updateLayout(pLayout);
        final Resource<Layout> resource = toResource(layout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public Resource<Layout> toResource(final Layout pElement, final Object... pExtras) {
        final Resource<Layout> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveLayout", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()));
        resourceService.addLink(resource, this.getClass(), "updateLayout", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()),
                                MethodParamFactory.build(Layout.class));
        return resource;
    }

}
