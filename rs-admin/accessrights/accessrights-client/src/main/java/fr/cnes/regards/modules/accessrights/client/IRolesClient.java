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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

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
@RequestMapping(value = "/roles", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IRolesClient { // NOSONAR

    public static final String PATH_BORROWABLE = "/borrowables";

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<Role>>> retrieveRoles();

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
     * Define the endpoint for retrieving the list of borrowable Roles for the current user.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = PATH_BORROWABLE)
    public ResponseEntity<List<Resource<Role>>> retrieveBorrowableRoles();

}