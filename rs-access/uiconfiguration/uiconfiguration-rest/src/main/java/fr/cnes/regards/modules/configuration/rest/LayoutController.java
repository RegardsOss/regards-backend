/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
@RequestMapping("/layouts")
public class LayoutController implements IResourceController<Layout> {

    @Autowired
    private ILayoutService layoutService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a {@link Layout}
     * @param applicationId
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM layout configuration for the given applicationId",
            role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Layout>> retrieveLayout(@PathVariable("applicationId") final String applicationId)
            throws EntityNotFoundException {
        final Layout layout = layoutService.retrieveLayout(applicationId);
        final Resource<Layout> resource = toResource(layout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to update {@link Layout}
     * @param applicationId
     * @param layout
     *
     * @return updated {@link Layout}
     * @throws EntityException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM layout configuration for the given applicationId",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Layout>> updateLayout(@PathVariable("applicationId") final String applicationId,
            @Valid @RequestBody final Layout layout) throws EntityException {
        final Layout updated = layoutService.updateLayout(layout);
        final Resource<Layout> resource = toResource(updated);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public Resource<Layout> toResource(final Layout element, final Object... extras) {
        final Resource<Layout> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveLayout", LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getApplicationId()));
        resourceService.addLink(resource, this.getClass(), "updateLayout", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, element.getApplicationId()),
                                MethodParamFactory.build(Layout.class));
        return resource;
    }

}
