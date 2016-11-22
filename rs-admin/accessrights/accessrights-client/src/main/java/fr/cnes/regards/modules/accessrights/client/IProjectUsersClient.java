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
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class IProjectUsersClient
 *
 * Feign client for rs-admin ProjectUsers controller.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IProjectUsersClient {

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<ProjectUser>>> retrieveProjectUserList();

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>email</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_email}", method = RequestMethod.GET)
    ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_email") String pUserEmail);

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT)
    ResponseEntity<Void> updateProjectUser(@PathVariable("user_id") Long pUserId,
            @RequestBody ProjectUser pUpdatedProjectUser);

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return
     */
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(@PathVariable("user_id") Long pUserId);

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData
     *            The {@link List} of {@link MetaData} to set
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.PUT)
    ResponseEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") Long pUserId,
            @Valid @RequestBody List<MetaData> pUpdatedUserMetaData);

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pLogin
     *            The {@link Account}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return the list of resources access
     */
    @ResponseBody
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.GET)
    ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserAccessRights(
            @PathVariable("user_login") String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) String pBorrowedRoleName);

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     */
    @ResponseBody
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.PUT)
    ResponseEntity<Void> updateProjectUserAccessRights(@PathVariable("user_login") String pLogin,
            @Valid @RequestBody List<ResourcesAccess> pUpdatedUserAccessRights);

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser} <code>login</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeProjectUserAccessRights(@PathVariable("user_login") String pUserLogin);

}
