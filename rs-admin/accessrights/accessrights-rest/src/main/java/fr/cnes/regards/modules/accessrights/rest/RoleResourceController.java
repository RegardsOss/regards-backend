/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Preconditions;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role resource management API
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(RoleResourceController.TYPE_MAPPING)
public class RoleResourceController implements IResourceController<ResourcesAccess> {

    /**
     * Controller base mapping
     */
    public static final String TYPE_MAPPING = "/roles/{role_name}/resources";

    /**
     * Single resource mapping
     */
    public static final String SINGLE_RESOURCE_MAPPING = "/{resources_access_id}";

    /**
     * Service handling roles.
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Service handling resources.
     */
    @Autowired
    private IResourcesService resourceService;

    /**
     * Resource service to manage visible hateoas links
     */
    @Autowired
    private IResourceService hateoasService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Get all resource accesses of a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<EntityModel<ResourcesAccess>>> getRoleResources(
        @PathVariable("role_name") String roleName) throws ModuleException {
        Role role = roleService.retrieveRole(roleName);
        Set<ResourcesAccess> resources = roleService.retrieveRoleResourcesAccesses(role.getId());
        return new ResponseEntity<>(toResources(resources, roleName), HttpStatus.OK);
    }

    @GetMapping(value = "/{microservice}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<ResourcesAccess>>> getRoleResourcesForMicroservice(
        @PathVariable("role_name") final String roleName, @PathVariable("microservice") final String microserviceName)
        throws ModuleException {
        Role role = roleService.retrieveRole(roleName);
        Set<ResourcesAccess> resources = roleService.retrieveRoleResourcesAccesses(role.getId())
                                                    .stream()
                                                    .filter(r -> r.getMicroservice().equals(microserviceName))
                                                    .collect(Collectors.toSet());
        return new ResponseEntity<>(toResources(resources, roleName), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Add access to one resource for a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<ResourcesAccess>> addRoleResource(@PathVariable("role_name") String roleName,
                                                                        @RequestBody
                                                                        @Valid ResourcesAccess newResourcesAccess)
        throws ModuleException {
        Role role = roleService.retrieveRole(roleName);
        roleService.addResourceAccesses(role.getId(), newResourcesAccess);
        return new ResponseEntity<>(toResource(newResourcesAccess, roleName), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = SINGLE_RESOURCE_MAPPING)
    @ResourceAccess(description = "Remove one resource access from a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteRoleResource(@PathVariable("role_name") String roleName,
                                                   @PathVariable("resources_access_id") Long resourcesAccessId)
        throws ModuleException {
        resourceService.removeRoleResourcesAccess(roleName, resourcesAccessId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public EntityModel<ResourcesAccess> toResource(ResourcesAccess resourcesAccess, Object... extras) {
        Preconditions.checkNotNull(extras);
        String roleName = (String) extras[0];

        EntityModel<ResourcesAccess> resource = hateoasService.toResource(resourcesAccess);
        hateoasService.addLink(resource,
                               this.getClass(),
                               "getRoleResources",
                               LinkRels.LIST,
                               MethodParamFactory.build(String.class, roleName));
        hateoasService.addLink(resource,
                               this.getClass(),
                               "addRoleResource",
                               LinkRels.CREATE,
                               MethodParamFactory.build(String.class, roleName),
                               MethodParamFactory.build(ResourcesAccess.class));

        if (isRemovable(resourcesAccess, roleName)) {
            hateoasService.addLink(resource,
                                   this.getClass(),
                                   "deleteRoleResource",
                                   LinkRels.DELETE,
                                   MethodParamFactory.build(String.class, roleName),
                                   MethodParamFactory.build(Long.class, resourcesAccess.getId()));
        }
        return resource;
    }

    /**
     * Removal of resource accesses is tricky: we can only remove a resource which is attached to PUBLIC if we are working on a native role.
     *
     * @return whether the resource access can be removed from the given role
     */
    private boolean isRemovable(ResourcesAccess resourcesAccess, String roleName) {
        try {
            Role currentRole = roleService.retrieveRole(roleName);
            if (!currentRole.isNative()) {
                Role publicRole = roleService.retrieveRole(DefaultRole.PUBLIC.name());
                Set<ResourcesAccess> publicResources = roleService.retrieveRoleResourcesAccesses(publicRole.getId());
                return !publicResources.contains(resourcesAccess); // on a custom role we can only remove a resource access if IT IS NOT one of public
            }
            return true; // we can remove any resources from any native role but PROJECT_ADMIN
        } catch (EntityNotFoundException e) {
            // This exception cannot occur as Role PUBLIC is a native role and not deletable!
            // Moreover, As we are at the end of execution fo other method using roleName if it doesn't represent
            // anything then it will be fired before here.
            return false;
        }
    }

}
