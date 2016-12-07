/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.ProjectUserWorkflowManager;

/**
 *
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 * @author Sébastien Binda
 *
 * @since 1.0-SNAPSHOT
 *
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/users")
public class ProjectUsersController {

    /**
     * Service handling project users
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Workflow manager for project users
     */
    @Autowired
    private ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return a {@link List} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveProjectUserList() {
        final List<ProjectUser> users = projectUserService.retrieveUserList();
        final List<Resource<ProjectUser>> resources = users.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pUserEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return a {@link ProjectUser}
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = "/{user_email}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_email") final String pUserEmail)
            throws EntityNotFoundException {
        final ProjectUser user = projectUserService.retrieveOneByEmail(pUserEmail);
        final Resource<ProjectUser> resource = new Resource<>(user);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @return void
     * @throws EntityException
     *             <br>
     *             {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> is different from the
     *             id of <code>pUpdatedProjectUser</code><br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the project user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateProjectUser(@PathVariable("user_id") final Long pUserId,
            @RequestBody final ProjectUser pUpdatedProjectUser) throws EntityException {
        projectUserService.updateUser(pUserId, pUpdatedProjectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return void
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} when the project user has a <code>status</code> not
     *             allowing removal<br>
     *             {@link EntityNotFoundException} user not found<br>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the project user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") final Long pUserId) throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pUserId);
        projectUserWorkflowManager.removeAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return a{@link List} of {@link MetaData}
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "retrieve the list of all metadata of the user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(
            @PathVariable("user_id") final Long pUserId) throws EntityNotFoundException {
        final List<MetaData> metaDatas = projectUserService.retrieveUserMetaData(pUserId);
        final List<Resource<MetaData>> resources = metaDatas.stream().map(m -> new Resource<>(m))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData
     *            The {@link List} of {@link MetaData} to set
     * @return void
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the list of all metadata of the user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") final Long pUserId,
            @Valid @RequestBody final List<MetaData> pUpdatedUserMetaData) throws EntityNotFoundException {
        projectUserService.updateUserMetaData(pUserId, pUpdatedUserMetaData);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @return void
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove all the metadata of the user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") final Long pUserId)
            throws EntityNotFoundException {
        projectUserService.removeUserMetaData(pUserId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pUserLogin
     *            The {@link Account}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return the {@link List} list of resources access
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} Thrown when the passed {@link Role} is not hierarchically
     *             inferior to the true {@link ProjectUser}'s <code>role</code>.<br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     */
    @ResponseBody
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of specific access rights and the role of the project user",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserAccessRights(
            @PathVariable("user_login") final String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) final String pBorrowedRoleName)
            throws EntityException {
        final List<ResourcesAccess> permissions = projectUserService.retrieveProjectUserAccessRights(pUserLogin,
                                                                                                     pBorrowedRoleName);

        final List<Resource<ResourcesAccess>> result = new ArrayList<>();
        for (final ResourcesAccess item : permissions) {
            result.add(new Resource<>(item));
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return void
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the list of specific user access rights", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateProjectUserAccessRights(@PathVariable("user_login") final String pLogin,
            @Valid @RequestBody final List<ResourcesAccess> pUpdatedUserAccessRights) throws EntityNotFoundException {
        projectUserService.updateUserAccessRights(pLogin, pUpdatedUserAccessRights);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     *
     * @param pUserLogin
     *            The {@link ProjectUser} <code>login</code>
     * @return void
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove all the specific access rights", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUserAccessRights(@PathVariable("user_login") final String pUserLogin)
            throws EntityNotFoundException {
        projectUserService.removeUserAccessRights(pUserLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
