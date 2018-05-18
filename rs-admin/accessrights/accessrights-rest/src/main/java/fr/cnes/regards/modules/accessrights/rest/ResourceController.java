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
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;

/**
 *
 * Resource management API
 *
 * Rest controller to access ResourcesAccess entities. ResourceAccess are the security configuration to allow access for
 * given roles to microservices endpoints. This configuration is made for each project of the regards instance.
 *
 * @author Sébastien Binda
 * @since 1.0-SNASHOT
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(value = ResourceController.TYPE_MAPPING)
public class ResourceController implements IResourceController<ResourcesAccess> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/resources";

    /**
     * Single resource mapping
     */
    public static final String RESOURCE_MAPPING = "/{resource_id}";

    /**
     * Business service
     */
    @Autowired
    private IResourcesService resourceService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService hateoasService;

    /**
     * Retrieve resource accesses available to the user
     *
     * @param pPageable
     *            pagination information
     * @param pPagedResourcesAssembler
     *            page assembler
     * @return list of user resource accesses
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve accessible resource accesses of the user among the system",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<ResourcesAccess>>> getAllResourceAccesses(final Pageable pPageable,
            final PagedResourcesAssembler<ResourcesAccess> pPagedResourcesAssembler) throws ModuleException {
        return new ResponseEntity<>(
                toPagedResources(resourceService.retrieveRessources(null, pPageable), pPagedResourcesAssembler),
                HttpStatus.OK);
    }

    /**
     *
     * Retrieve the ResourceAccess with given id {@link Long} exists.
     *
     * @param pResourceId
     *            resource id
     * @return {@link ResourcesAccess}
     * @throws ModuleException
     *             Exception if resource with given id does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = RESOURCE_MAPPING)
    @ResourceAccess(description = "Retrieve all resource accesses of the REGARDS system", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<ResourcesAccess>> getResourceAccess(
            @PathVariable("resource_id") final Long pResourceId) throws ModuleException {
        return new ResponseEntity<>(toResource(resourceService.retrieveRessource(pResourceId)), HttpStatus.OK);
    }

    /**
     *
     * Update given resource access informations
     *
     * @param pResourceId
     *            Resource access identifier
     * @param pResourceAccessToUpdate
     *            Resource access to update
     * @return updated ResourcesAccess
     * @throws ModuleException
     *             Exception if resource with given id does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.PUT, value = RESOURCE_MAPPING)
    @ResourceAccess(description = "Update access to a given resource", role = DefaultRole.ADMIN)
    public ResponseEntity<Resource<ResourcesAccess>> updateResourceAccess(
            @PathVariable("resource_id") final Long pResourceId,
            @Valid @RequestBody final ResourcesAccess pResourceAccessToUpdate) throws ModuleException {
        if ((pResourceAccessToUpdate.getId() == null) || !pResourceAccessToUpdate.getId().equals(pResourceId)) {
            throw new EntityInvalidException(
                    String.format("Resource to update with id %d do not match the required resource id %d",
                                  pResourceAccessToUpdate.getId(), pResourceId));
        }
        return new ResponseEntity<>(toResource(resourceService.updateResource(pResourceAccessToUpdate)), HttpStatus.OK);
    }

    @Override
    public Resource<ResourcesAccess> toResource(final ResourcesAccess pElement, final Object... pExtras) {
        final Resource<ResourcesAccess> resource = hateoasService.toResource(pElement);
        hateoasService.addLink(resource, this.getClass(), "getAllResourceAccesses", LinkRels.LIST,
                               MethodParamFactory.build(Pageable.class),
                               MethodParamFactory.build(PagedResourcesAssembler.class));
        hateoasService.addLink(resource, this.getClass(), "getResourceAccess", LinkRels.SELF,
                               MethodParamFactory.build(Long.class, pElement.getId()));
        hateoasService.addLink(resource, this.getClass(), "updateResourceAccess", LinkRels.UPDATE,
                               MethodParamFactory.build(Long.class, pElement.getId()),
                               MethodParamFactory.build(pElement.getClass()));
        return resource;
    }
}
