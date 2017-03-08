/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;

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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
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
@RequestMapping(RolesController.REQUEST_MAPPING_ROOT)
public class RolesController implements IResourceController<Role> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/roles";

    public static final String PATH_BORROWABLE = "/borrowables";

    public static final String PATH_BORROWABLE_TARGET = PATH_BORROWABLE + "/{target}";

    @Autowired
    private IRoleService roleService;

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
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of roles", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<Role>>> retrieveRoles() {
        final Set<Role> roles = roleService.retrieveRoles();
        return new ResponseEntity<>(toResources(roles), HttpStatus.OK);
    }

    /**
     * Define the endpoint for retrieving the list of borrowable Roles for the current user.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     * @throws JwtException
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = PATH_BORROWABLE)
    @ResourceAccess(description = "Retrieve the list of borrowable roles for the current user",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<Resource<Role>>> retrieveBorrowableRoles() throws JwtException {
        final Set<Role> roles = roleService.retrieveBorrowableRoles();
        return new ResponseEntity<>(toResources(roles), HttpStatus.OK);
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
    @ResponseBody
    @RequestMapping(value = "/{role_name}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve a role by id", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<Role>> retrieveRole(@PathVariable("role_name") final String pRoleName)
            throws EntityNotFoundException {
        final Role role = roleService.retrieveRole(pRoleName);
        return new ResponseEntity<>(toResource(role), HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @param pUpdatedRole
     *            The new {@link Role}
     * @return Updated {@link Role}
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
    public ResponseEntity<Resource<Role>> updateRole(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final Role pUpdatedRole) throws EntityException {
        final Role updatedRole = roleService.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(toResource(updatedRole), HttpStatus.OK);
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

    @Override
    public Resource<Role> toResource(final Role pElement, final Object... pExtras) {
        Resource<Role> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, this.getClass(), "retrieveRole", LinkRels.SELF,
                                    MethodParamFactory.build(String.class, pElement.getName()));
            resourceService.addLink(resource, this.getClass(), "updateRole", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, pElement.getId()),
                                    MethodParamFactory.build(Role.class));
            resourceService.addLink(resource, this.getClass(), "removeRole", LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "retrieveRoles", LinkRels.LIST);
        }
        return resource;
    }

}