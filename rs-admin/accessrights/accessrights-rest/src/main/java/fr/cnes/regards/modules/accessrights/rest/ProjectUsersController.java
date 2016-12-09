/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
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
@RequestMapping(ProjectUsersController.REQUEST_MAPPING_ROOT)
public class ProjectUsersController implements IResourceController<ProjectUser> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/users";

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
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Service handling roles.
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return a {@link List} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveProjectUserList(
            @RequestParam(name = "status", required = false) final String pStatus) {
        List<ProjectUser> users;
        if (pStatus == null) {
            users = projectUserService.retrieveUserList();
        } else {
            users = projectUserService.retrieveUserList(UserStatus.valueOf(pStatus));
        }
        return new ResponseEntity<>(toResources(users), HttpStatus.OK);
    }

    /**
     * Retrieve all users with a pending access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(value = "/pendingaccesses", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        final List<ProjectUser> projectUsers = projectUserService.retrieveAccessRequestList();
        return new ResponseEntity<>(toResources(projectUsers), HttpStatus.OK);
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
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(
            @PathVariable("user_email") final String pUserEmail) throws EntityNotFoundException {
        final ProjectUser user = projectUserService.retrieveOneByEmail(pUserEmail);
        return new ResponseEntity<>(toResource(user), HttpStatus.OK);
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
    public ResponseEntity<Resource<ProjectUser>> updateProjectUser(@PathVariable("user_id") final Long pUserId,
            @RequestBody final ProjectUser pUpdatedProjectUser) throws EntityException {
        final ProjectUser updatedUser = projectUserService.updateUser(pUserId, pUpdatedProjectUser);
        return new ResponseEntity<>(toResource(updatedUser), HttpStatus.OK);
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
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.GET)
    @ResourceAccess(
            description = "Retrieve the list of project users (crawls through parents' hierarachy) of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId) throws EntityNotFoundException {
        final List<ProjectUser> projectUserList = roleService.retrieveRoleProjectUserList(pRoleId);
        return new ResponseEntity<>(toResources(projectUserList), HttpStatus.OK);
    }

    @Override
    public Resource<ProjectUser> toResource(final ProjectUser pElement, final Object... pExtras) {
        Resource<ProjectUser> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUser", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "updateProjectUser", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, pElement.getId()),
                                    MethodParamFactory.build(Account.class));
            resourceService.addLink(resource, this.getClass(), "removeProjectUser", LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUserList", LinkRels.LIST,
                                    MethodParamFactory.build(String.class, pElement.getStatus().toString()));
        }
        return resource;
    }

}
