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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Role management API
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
@RestController
@RequestMapping(RoleController.TYPE_MAPPING)
public class RoleController implements IResourceController<Role> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/roles";

    /**
     * Mapping for managing a role mapping for requests of this rest controller
     */
    public static final String ROLE_MAPPING = "/{role_name}";

    public static final String ROLE_DESCENDANTS = ROLE_MAPPING + "/descendants";

    public static final String ROLE_ASCENDANTS = ROLE_MAPPING + "/ascendants";

    public static final String SHOULD_ACCESS_TO_RESOURCE = "/include" + ROLE_MAPPING;

    /**
     * Mapping for retrieving borrowable role of the current user
     */
    public static final String BORROWABLE_MAPPING = "/borrowables";

    /**
     * Mapping for retrieving all roles that can access the specified resource
     */
    public static final String ROLE_WITH_RESOURCE_MAPPING = "/resources/{resourceId}";

    /**
     * {@link RoleService}
     */
    @Autowired
    private IRoleService roleService;

    /**
     * {@link IProjectUserService} instance
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @GetMapping
    @Operation(summary = "Get roles", description = "Return a list of roles")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All roles were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve the list of roles sorted by name", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<Role>>> getAllRoles() {

        return new ResponseEntity<>(toResources(roleService.retrieveRoles()), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the list of borrowable Roles for the current user.
     * The borowalble roles contains at least the authenticated user own role.
     *
     * @return list of borrowable roles for current authenticated user
     */
    @RequestMapping(method = RequestMethod.GET, path = BORROWABLE_MAPPING)
    @ResourceAccess(description = "Retrieve the list of borrowable roles for the current user",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<List<EntityModel<Role>>> getBorrowableRoles() {
        return new ResponseEntity<>(toResources(roleService.retrieveBorrowableRoles()), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the list of roles that can access the specified resource.
     *
     * @return list of borrowable roles for current authenticated user
     */
    @RequestMapping(method = RequestMethod.GET, path = ROLE_WITH_RESOURCE_MAPPING)
    @ResourceAccess(description = "Retrieve the list of roles associated to the given resource",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<Role>>> getRolesAccesingResource(
        @PathVariable("resourceId") Long resourceId) {
        return new ResponseEntity<>(toResources(roleService.retrieveRolesWithResource(resourceId)), HttpStatus.OK);
    }

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param newRole The new {@link Role} values
     * @return The created {@link Role}
     * @throws EntityException Thrown if a {@link Role} with same <code>id</code> already exists
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<Role>> createRole(@RequestBody Role newRole) throws EntityException {
        return new ResponseEntity<>(toResource(roleService.createRole(newRole)), HttpStatus.CREATED);
    }

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param roleName The {@link Role}'s <code>name</code>
     * @return The {@link Role} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException when no role with passed name could be found
     */
    @RequestMapping(method = RequestMethod.GET, value = ROLE_MAPPING)
    @ResourceAccess(description = "Retrieve a role by name", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<Role>> retrieveRole(@PathVariable("role_name") final String roleName)
        throws EntityNotFoundException {
        return new ResponseEntity<>(toResource(roleService.retrieveRole(roleName)), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the descendants {@link Role}s of passed role through its name
     *
     * @return the ascendants wrapped into a {@link ResponseEntity}
     * @throws EntityNotFoundException if given role does not exist
     */
    @ResourceAccess(description = "Retrieve a role's descendants", role = DefaultRole.EXPLOIT)
    @RequestMapping(method = RequestMethod.GET, path = ROLE_DESCENDANTS)
    public ResponseEntity<Set<Role>> retrieveRoleDescendants(@PathVariable("role_name") String roleName)
        throws EntityNotFoundException {
        return new ResponseEntity<>(roleService.getDescendants(roleService.retrieveRole(roleName)), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the ascendants {@link Role}s of passed role through its name
     *
     * @return the ascendants wrapped into a {@link ResponseEntity}
     * @throws EntityNotFoundException if given role does not exist
     */
    @ResourceAccess(description = "Retrieve a role's ascendants", role = DefaultRole.EXPLOIT)
    @RequestMapping(method = RequestMethod.GET, path = ROLE_ASCENDANTS)
    public ResponseEntity<Set<Role>> retrieveRoleAscendants(@PathVariable("role_name") String roleName)
        throws EntityNotFoundException {
        return new ResponseEntity<>(roleService.getAscendants(roleService.retrieveRole(roleName)), HttpStatus.OK);
    }

    /**
     * Define the endpoint to determine if the provided ${@link Role} is inferior to the one brought by the current request
     *
     * @param roleName that should be inferior
     * @return true when the current role should have access to something requiring at least the provided role
     * @throws EntityNotFoundException if some role does not exists
     */
    @ResourceAccess(description = "Return true if the role provided is included by the current role",
                    role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = SHOULD_ACCESS_TO_RESOURCE)
    public ResponseEntity<Boolean> shouldAccessToResourceRequiring(@PathVariable("role_name") String roleName)
        throws EntityNotFoundException {
        Optional<Role> cr = roleService.getCurrentRole();
        boolean result = false;
        if (cr.isPresent()) {
            result = cr.get().getName().equals(roleName) || roleService.isCurrentRoleSuperiorTo(roleName);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param roleName    The {@link Role}
     * @param updatedRole The new {@link Role}
     * @return Updated {@link Role}
     * @throws ModuleException <br>
     *                         {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br>
     *                         {@link EntityOperationForbiddenException} Thrown when <code>pRoleId</code> is different from the id
     *                         of <code>pUpdatedRole</code><br>
     */
    @RequestMapping(method = RequestMethod.PUT, value = ROLE_MAPPING)
    @ResourceAccess(description = "Update the role of role_name with passed body", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<Role>> updateRole(@PathVariable("role_name") String roleName,
                                                        @Valid @RequestBody Role updatedRole) throws ModuleException {
        return new ResponseEntity<>(toResource(roleService.updateRole(roleName, updatedRole)), HttpStatus.OK);
    }

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param roleName The {@link Role}'s <code>name</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws ModuleException if the updated role is native. Native roles should not be modified.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = ROLE_MAPPING)
    @ResourceAccess(description = "Remove the role of role_name", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeRole(@PathVariable("role_name") String roleName) throws ModuleException {
        roleService.removeRole(roleName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<Role> toResource(Role role, Object... extras) {
        EntityModel<Role> resource = null;
        if ((role != null) && (role.getId() != null)) {
            resource = resourceService.toResource(role);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "retrieveRole",
                                    LinkRels.SELF,
                                    MethodParamFactory.build(String.class, role.getName()));
            // Disable eddition and deletion of native roles.
            if (!role.isNative()) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "updateRole",
                                        LinkRels.UPDATE,
                                        MethodParamFactory.build(String.class, role.getName()),
                                        MethodParamFactory.build(Role.class));
                if (isDeletable(role)) {
                    resourceService.addLink(resource,
                                            this.getClass(),
                                            "removeRole",
                                            LinkRels.DELETE,
                                            MethodParamFactory.build(String.class, role.getName()));
                }
            }
            if (!(RoleAuthority.isProjectAdminRole(role.getName())
                  || RoleAuthority.isInstanceAdminRole(role.getName()))) {

                //we add the link to manage a role resources accesses except for PROJECT_ADMIN and INSTANCE_ADMIN
                resourceService.addLink(resource,
                                        RoleResourceController.class,
                                        "getRoleResources",
                                        LinkRelation.of("manage-resource-access"),
                                        MethodParamFactory.build(String.class, role.getName()));
            }
            resourceService.addLink(resource, this.getClass(), "getAllRoles", LinkRels.LIST);
            resourceService.addLink(resource, this.getClass(), "getBorrowableRoles", LinkRelation.of("borrowable"));
        }
        return resource;
    }

    private boolean isDeletable(Role role) {
        Collection<ProjectUser> users = projectUserService.retrieveUserByRole(role);
        return users.isEmpty();
    }

}