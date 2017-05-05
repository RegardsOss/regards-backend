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
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * User resource management API client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = IUserResourceClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IUserResourceClient {

    /**
     * Controller base mapping
     */
    public static final String TYPE_MAPPING = "/users/{user_email}/resources";

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pUserLogin
     *            The {@link Account}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return the {@link List} list of resources access
     *
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserResources(
            @PathVariable("user_email") final String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) final String pBorrowedRoleName);

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return void
     */
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the list of specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateProjectUserResources(@PathVariable("user_email") final String pLogin,
            @Valid @RequestBody final List<ResourcesAccess> pUpdatedUserAccessRights);

    /**
     * Remove all specific user accesses
     *
     * @param pUserLogin
     *            user email
     * @return {@link Void}
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @ResourceAccess(description = "Remove all specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUserResources(@PathVariable("user_email") final String pUserLogin);
}
