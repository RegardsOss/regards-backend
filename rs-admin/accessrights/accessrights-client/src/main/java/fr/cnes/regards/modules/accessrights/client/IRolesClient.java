/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleDTO;

/**
 *
 * Class IRolesClient
 *
 * Feign client for rs-admin Roles controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/roles", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IRolesClient {

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<RoleDTO>>> retrieveRoleList();

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Resource<Role>> createRole(@Valid @RequestBody Role pNewRole);

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link Role} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{role_name}", method = RequestMethod.GET)
    ResponseEntity<Resource<Role>> retrieveRole(@PathVariable("role_name") String pRoleName);

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
    ResponseEntity<Void> updateRole(@PathVariable("role_id") Long pRoleId, @Valid @RequestBody Role pUpdatedRole);

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeRole(@PathVariable("role_id") Long pRoleId);

    /**
     * Define the endpoint for returning the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of permissions as {@link ResourcesAccess} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.GET)
    ResponseEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") Long pRoleId);

    /**
     * Define the endpoint for setting the passed {@link List} of {@link ResourcesAccess} onto the {@link role} of
     * passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccessList
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.PUT)
    ResponseEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") Long pRoleId,
            @Valid @RequestBody List<ResourcesAccess> pResourcesAccessList);

    /**
     * Define the endpoint for clearing the {@link List} of {@link ResourcesAccess} of the {@link Role} with passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.DELETE)
    ResponseEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") Long pRoleId);

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
    ResponseEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(@PathVariable("role_id") Long pRoleId);

}