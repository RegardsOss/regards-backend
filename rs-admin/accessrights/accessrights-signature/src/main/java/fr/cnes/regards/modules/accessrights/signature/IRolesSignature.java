/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * Define the common interface of REST clients for {@link Role}s.
 *
 * @author CS SI
 */
@RequestMapping("/roles")
public interface IRolesSignature {

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link HttpEntity}
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<Role>>> retrieveRoleList();

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     * @throws AlreadyExistingException
     *             Thrown if a {@link Role} with same <code>id</code> already exists
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Role>> createRole(@Valid @RequestBody Role pNewRole) throws AlreadyExistingException;

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link Role} wrapped in an {@link HttpEntity}
     */
    @RequestMapping(value = "/{role_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Role>> retrieveRole(@PathVariable("role_id") Long pRoleId);

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @param pUpdatedRole
     *            The new {@link Role}
     * @throws InvalidValueException
     *             Thrown when <code>pRoleId</code> is different from the id of <code>pUpdatedRole</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     * @return {@link Void} wrapped in an {@link HttpEntity}
     */
    @RequestMapping(value = "/{role_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateRole(@PathVariable("role_id") Long pRoleId, @Valid @RequestBody Role pUpdatedRole)
            throws EntityNotFoundException, InvalidValueException;

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return {@link Void} wrapped in an {@link HttpEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{role_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeRole(@PathVariable("role_id") Long pRoleId) throws EntityNotFoundException;

    /**
     * Define the endpoint for returning the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of permissions as {@link ResourcesAccess} wrapped in an {@link HttpEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(@PathVariable("role_id") Long pRoleId)
            throws EntityNotFoundException;

    /**
     * Define the endpoint for setting the passed {@link List} of {@link ResourcesAccess} onto the {@link role} of
     * passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccessList
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return {@link Void} wrapped in an {@link HttpEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") Long pRoleId,
            @Valid @RequestBody List<ResourcesAccess> pResourcesAccessList) throws EntityNotFoundException;

    /**
     * Define the endpoint for clearing the {@link List} of {@link ResourcesAccess} of the {@link Role} with passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @return {@link Void} wrapped in an {@link HttpEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") Long pRoleId) throws EntityNotFoundException;

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUser} for the {@link Role} of passed
     * <code>id</code> by crawling through parents' hierarachy.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of {@link ProjectUser} wrapped in an {@link HttpEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{role_id}/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(@PathVariable("role_id") Long pRoleId);
}
