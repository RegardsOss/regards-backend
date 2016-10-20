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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.accessrights.domain.Couple;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * Define the common interface of REST clients for {@link ProjectUser}s.
 *
 * @author CS SI
 */
@RequestMapping("/users")
public interface IProjectUsersSignature {

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveProjectUserList();

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     */
    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @throws InvalidValueException
     *             Thrown when <code>pUserId</code> is different from the id of <code>pUpdatedProjectUser</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUser(@PathVariable("user_id") Long pUserId,
            @RequestBody ProjectUser pUpdatedProjectUser) throws InvalidValueException, EntityNotFoundException;

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     */
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(@PathVariable("user_id") Long pUserId)
            throws EntityNotFoundException;

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData
     *            The {@link List} of {@link MetaData} to set
     */
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") Long pUserId,
            @Valid @RequestBody List<MetaData> pUpdatedUserMetaData) throws EntityNotFoundException;

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") Long pUserId) throws EntityNotFoundException;

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pLogin
     *            The {@link Account}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return
     * @throws InvalidValueException
     *             Thrown when the passed {@link Role} is not hierarchically inferior to the true {@link ProjectUser}'s
     *             <code>role</code>.
     */
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Couple<List<ResourcesAccess>, Role>>> retrieveProjectUserAccessRights(
            @PathVariable("user_login") String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) String pBorrowedRoleName)
            throws InvalidValueException;

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUserAccessRights(@PathVariable("user_login") String pLogin,
            @Valid @RequestBody List<ResourcesAccess> pUpdatedUserAccessRights) throws EntityNotFoundException;

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser} <code>login</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUserAccessRights(@PathVariable("user_login") String pUserLogin)
            throws EntityNotFoundException;
}
