/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleDTO;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 *
 * Class RolesController
 *
 * Endpoints to manage Role entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(value = "/roles")
public class RolesController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesController.class);

    @Autowired
    private IRoleService roleService;

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of roles", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<RoleDTO>>> retrieveRoleList() {
        final List<Role> roles = roleService.retrieveRoleList();
        final List<RoleDTO> rolesDTO = new ArrayList<>();
        roles.forEach(r -> rolesDTO.add(new RoleDTO(r)));
        final List<Resource<RoleDTO>> resources = rolesDTO.stream().map(r -> new Resource<>(r))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     * @throws EntityAlreadyExistsException
     *             Thrown if a {@link Role} with same <code>id</code> already exists
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<Role>> createRole(@Valid @RequestBody final Role pNewRole)
            throws EntityAlreadyExistsException {
        final Role created = roleService.createRole(pNewRole);
        final Resource<Role> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link Role} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             when no role with passed name could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{role_name}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve a role by id", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<Role>> retrieveRole(@PathVariable("role_name") final String pRoleName)
            throws EntityNotFoundException {
        final Role role = roleService.retrieveRole(pRoleName);
        final Resource<Role> resource = new Resource<>(role);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @param pUpdatedRole
     *            The new {@link Role}
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br>
     *             {@link EntityOperationForbiddenException} Thrown when <code>pRoleId</code> is different from the id
     *             of <code>pUpdatedRole</code><br>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the role of role_id with passed body", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateRole(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final Role pUpdatedRole) throws EntityException {
        roleService.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityOperationForbiddenException
     *             if the updated role is native. Native roles should not be modified.
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Remove the role of role_id", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeRole(@PathVariable("role_id") final Long pRoleId)
            throws EntityOperationForbiddenException {
        roleService.removeRole(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Define the endpoint for returning the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of permissions as {@link ResourcesAccess} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") final Long pRoleId) throws EntityNotFoundException {
        final List<ResourcesAccess> resourcesAccesses = roleService.retrieveRoleResourcesAccessList(pRoleId);
        final List<Resource<ResourcesAccess>> resources = resourcesAccesses.stream().map(ra -> new Resource<>(ra))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Define the endpoint for setting the passed {@link List} of {@link ResourcesAccess} onto the {@link role} of
     * passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccessList
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.PUT)
    @ResourceAccess(description = "Incrementally update the list of permissions of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final List<ResourcesAccess> pResourcesAccessList) throws EntityNotFoundException {
        roleService.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Define the endpoint for clearing the {@link List} of {@link ResourcesAccess} of the {@link Role} with passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Clear the list of permissions of the", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId)
            throws EntityNotFoundException {
        roleService.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUser} for the {@link Role} of passed
     * <code>id</code> by crawling through parents' hierarachy.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of {@link ProjectUser} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/users", method = RequestMethod.GET)
    @ResourceAccess(
            description = "Retrieve the list of project users (crawls through parents' hierarachy) of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId) {
        List<ProjectUser> projectUserList = new ArrayList<>();
        try {
            projectUserList = roleService.retrieveRoleProjectUserList(pRoleId);
        } catch (final EntityNotFoundException e) {
            LOG.error("Unable to retrieve the project user list", e);
        }
        final List<Resource<ProjectUser>> resources = projectUserList.stream().map(pu -> new Resource<>(pu))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

}