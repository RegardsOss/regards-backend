/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import java.util.List;

import org.springframework.hateoas.PagedResources;
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
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

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
    ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveProjectUserList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     * Retrieve all users with a pending access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(value = "/pendingaccesses", method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

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
    ResponseEntity<Resource<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long pUserId,
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
     * retrieveRoleProjectUserList
     *
     * @param pRoleId
     *            role identifier to retrieve users.
     * @param pPage
     *            page index
     * @param pSize
     *            page size
     * @return {@link PagedResources} of {@link ProjectUser}
     * @since 1.0-SNAPSHOT
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

}
