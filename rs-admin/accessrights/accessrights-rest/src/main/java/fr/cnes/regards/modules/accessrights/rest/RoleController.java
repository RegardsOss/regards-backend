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
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Role management API
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
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
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of roles", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<Role>>> getAllRoles() {
        final Set<Role> roles = roleService.retrieveRoles();
        return new ResponseEntity<>(toResources(roles), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the list of borrowable Roles for the current user.
     *
     * @return list of borrowable roles for current authenticated user
     */
    @RequestMapping(method = RequestMethod.GET, path = BORROWABLE_MAPPING)
    @ResourceAccess(description = "Retrieve the list of borrowable roles for the current user",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<Resource<Role>>> getBorrowableRoles() throws ModuleException {
        final Set<Role> roles = roleService.retrieveBorrowableRoles();
        return new ResponseEntity<>(toResources(roles), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the list of roles that can access the specified resource.
     *
     * @param pResourceId
     * @return list of borrowable roles for current authenticated user
     */
    @RequestMapping(method = RequestMethod.GET, path = ROLE_WITH_RESOURCE_MAPPING)
    @ResourceAccess(description = "Retrieve the list of roles associated to the given resource",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<Role>>> getRolesAccesingResource(
            @PathVariable("resourceId") final Long pResourceId) {
        final Set<Role> roles = roleService.retrieveRolesWithResource(pResourceId);
        return new ResponseEntity<>(toResources(roles), HttpStatus.OK);
    }

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     * @throws EntityException
     *             Thrown if a {@link Role} with same <code>id</code> already exists
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<Role>> createRole(@RequestBody final Role pNewRole) throws EntityException {
        final Role created = roleService.createRole(pNewRole);
        return new ResponseEntity<>(toResource(created), HttpStatus.CREATED);
    }

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleName
     *            The {@link Role}'s <code>name</code>
     * @return The {@link Role} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             when no role with passed name could be found
     */
    @RequestMapping(method = RequestMethod.GET, value = ROLE_MAPPING)
    @ResourceAccess(description = "Retrieve a role by name", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<Role>> retrieveRole(@PathVariable("role_name") final String pRoleName)
            throws EntityNotFoundException {
        final Role role = roleService.retrieveRole(pRoleName);
        return new ResponseEntity<>(toResource(role), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the descendnats {@link Role}s of passed role through its name
     * @return the ascendants wrapped into a {@link ResponseEntity}
     * @throws EntityNotFoundException if given role does not exists
     */
    @ResourceAccess(description = "Retrieve a role descendants")
    @RequestMapping(method = RequestMethod.GET, path = ROLE_DESCENDANTS)
    public ResponseEntity<Set<Role>> retrieveRoleDescendants(@PathVariable("role_name") String roleName)
            throws EntityNotFoundException {
        Role role = roleService.retrieveRole(roleName);
        return new ResponseEntity<>(roleService.getDescendants(role), HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleName
     *            The {@link Role}
     * @param pUpdatedRole
     *            The new {@link Role}
     * @return Updated {@link Role}
     * @throws ModuleException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br>
     *             {@link EntityOperationForbiddenException} Thrown when <code>pRoleId</code> is different from the id
     *             of <code>pUpdatedRole</code><br>
     */
    @RequestMapping(method = RequestMethod.PUT, value = ROLE_MAPPING)
    @ResourceAccess(description = "Update the role of role_name with passed body", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<Role>> updateRole(@PathVariable("role_name") final String pRoleName,
            @Valid @RequestBody final Role pUpdatedRole) throws ModuleException {
        final Role updatedRole = roleService.updateRole(pRoleName, pUpdatedRole);
        return new ResponseEntity<>(toResource(updatedRole), HttpStatus.OK);
    }

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleName
     *            The {@link Role}'s <code>name</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws ModuleException
     *             if the updated role is native. Native roles should not be modified.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = ROLE_MAPPING)
    @ResourceAccess(description = "Remove the role of role_name", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeRole(@PathVariable("role_name") final String pRoleName) throws ModuleException {
        roleService.removeRole(pRoleName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<Role> toResource(final Role role, final Object... pExtras) {
        Resource<Role> resource = null;
        if ((role != null) && (role.getId() != null)) {
            resource = resourceService.toResource(role);
            resourceService.addLink(resource, this.getClass(), "retrieveRole", LinkRels.SELF,
                                    MethodParamFactory.build(String.class, role.getName()));
            // Disable eddition and deletion of native roles.
            if (!role.isNative()) {
                resourceService.addLink(resource, this.getClass(), "updateRole", LinkRels.UPDATE,
                                        MethodParamFactory.build(String.class, role.getName()),
                                        MethodParamFactory.build(Role.class));
                if (isDeletable(role)) {
                    resourceService.addLink(resource, this.getClass(), "removeRole", LinkRels.DELETE,
                                            MethodParamFactory.build(String.class, role.getName()));
                }
            }
            if (!(RoleAuthority.isProjectAdminRole(role.getName()) || RoleAuthority.isInstanceAdminRole(role.getName()))) {

                //we add the link to manage a role resources accesses except for PROJECT_ADMIN and INSTANCE_ADMIN
                resourceService
                        .addLink(resource, RoleResourceController.class, "getRoleResources", "manage-resource-access",
                                 MethodParamFactory.build(String.class, role.getName()));
            }
            resourceService.addLink(resource, this.getClass(), "getAllRoles", LinkRels.LIST);
            resourceService.addLink(resource, this.getClass(), "getBorrowableRoles", "borrowable");
        }
        return resource;
    }

    private boolean isDeletable(Role role) {
        Collection<ProjectUser> users = projectUserService.retrieveUserByRole(role);
        return users.isEmpty();
    }

}