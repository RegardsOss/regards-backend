/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(ProjectUsersController.TYPE_MAPPING)
public class ProjectUsersController implements IResourceController<ProjectUser> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/users";

    /**
     * Relative path to the endpoints managing a single project user
     */
    public static final String USER_ID_RELATIVE_PATH = "/{user_id}";

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
     * Retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return a {@link List} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveProjectUserList(
            @RequestParam(name = "status", required = false) final String pStatus, final Pageable pPageable,
            final PagedResourcesAssembler<ProjectUser> pPagedResourcesAssembler) {
        Page<ProjectUser> users;
        if (pStatus == null) {
            users = projectUserService.retrieveUserList(pPageable);
        } else {
            users = projectUserService.retrieveUserList(UserStatus.valueOf(pStatus), pPageable);
        }
        return new ResponseEntity<>(toPagedResources(users, pPagedResourcesAssembler), HttpStatus.OK);
    }

    /**
     * Retrieve all users with a pending access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(value = "/pendingaccesses", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveAccessRequestList(final Pageable pPageable,
            final PagedResourcesAssembler<ProjectUser> pPagedResourcesAssembler) {
        final Page<ProjectUser> projectUsers = projectUserService.retrieveAccessRequestList(pPageable);
        return new ResponseEntity<>(toPagedResources(projectUsers, pPagedResourcesAssembler), HttpStatus.OK);
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return a {@link ProjectUser}
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") final Long pUserId)
            throws EntityNotFoundException {
        final ProjectUser user = projectUserService.retrieveUser(pUserId);
        return new ResponseEntity<>(toResource(user), HttpStatus.OK);
    }

    /**
     * Retrieve the {@link ProjectUser} of current authenticated user
     *
     * @return a {@link ProjectUser}
     * @throws EntityNotFoundException
     * @throws EntityOperationForbiddenException
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the current authenticated project user and only display  metadata",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Resource<ProjectUser>> retrieveCurrentProjectUser()
            throws EntityNotFoundException, EntityOperationForbiddenException {
        final String curentUserEmail = authResolver.getUser();
        if ((curentUserEmail == null) || curentUserEmail.isEmpty()) {
            throw new EntityOperationForbiddenException("Unable to retrieve current authenticated user.");

        }
        final ProjectUser user = projectUserService.retrieveOneByEmail(curentUserEmail);
        return new ResponseEntity<>(toResource(user), HttpStatus.OK);
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserEmail
     *            The {@link ProjectUser}'s <code>id</code>
     * @return a {@link ProjectUser}
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = "/email/{user_email}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUserByEmail(
            @PathVariable("user_email") final String pUserEmail) throws EntityNotFoundException {
        final ProjectUser user = projectUserService.retrieveOneByEmail(pUserEmail);
        return new ResponseEntity<>(toResource(user), HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/email/{user_email}/admin", method = RequestMethod.GET)
    @ResourceAccess(description = "tell if user has role admin", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") final String userEmail)
            throws EntityNotFoundException {
        final ProjectUser user = projectUserService.retrieveOneByEmail(userEmail);
        if (user.getRole().getName().equals(DefaultRole.INSTANCE_ADMIN.toString())
                || user.getRole().getName().equals(DefaultRole.ADMIN.toString())
                || (user.getRole().getName().equals(DefaultRole.PROJECT_ADMIN.toString()))
                || ((user.getRole().getParentRole() != null)
                        && user.getRole().getParentRole().getName().equals(DefaultRole.ADMIN.toString()))) {
            return new ResponseEntity<>(true, HttpStatus.OK);
        }
        return new ResponseEntity<>(false, HttpStatus.OK);
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
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "update the project user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ProjectUser>> updateProjectUser(@PathVariable("user_id") final Long pUserId,
            @RequestBody final ProjectUser pUpdatedProjectUser) throws EntityException {
        final ProjectUser updatedUser = projectUserService.updateUserInfos(pUserId, pUpdatedProjectUser);
        return new ResponseEntity<>(toResource(updatedUser), HttpStatus.OK);
    }

    /**
     * Update the {@link ProjectUser} of current projet user authenticated.
     *
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @return void
     * @throws EntityException
     *             <br>
     *             {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> is different from the
     *             id of <code>pUpdatedProjectUser</code><br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     *             {@link EntityOperationForbiddenException} Thrown when the user to update is not the current
     *             authenticated user<br>
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the current authenticated project user", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Resource<ProjectUser>> updateCurrentProjectUser(
            @RequestBody final ProjectUser pUpdatedProjectUser) throws EntityException {

        final ProjectUser user = projectUserService.retrieveCurrentUser();

        if (!user.getId().equals(pUpdatedProjectUser.getId())) {
            throw new EntityOperationForbiddenException("You are only allowed to update your own user properties.");
        }

        final ProjectUser updatedUser = projectUserService.updateUserInfos(user.getId(), pUpdatedProjectUser);
        return new ResponseEntity<>(toResourceRegisteredUser(updatedUser), HttpStatus.OK);

    }

    /**
     * Create a new user by bypassing registration process (accounts and projectUser validation)
     *
     * @param pDto
     *            A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException
     *             if error occurs.
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a projectUser by bypassing registration process (Administrator feature)",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ProjectUser>> createUser(@Valid @RequestBody final AccessRequestDto pDto)
            throws EntityException {
        final ProjectUser userCreated = projectUserService.createProjectUser(pDto);
        return new ResponseEntity<>(toResource(userCreated), HttpStatus.CREATED);
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
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.DELETE)
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
            description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId, final Pageable pPageable,
            final PagedResourcesAssembler<ProjectUser> pPagedResourcesAssembler) throws EntityNotFoundException {
        final Page<ProjectUser> projectUserList = roleService.retrieveRoleProjectUserList(pRoleId, pPageable);
        return new ResponseEntity<>(toPagedResources(projectUserList, pPagedResourcesAssembler), HttpStatus.OK);
    }

    @Override
    public Resource<ProjectUser> toResource(final ProjectUser pElement, final Object... pExtras) {
        Resource<ProjectUser> resource = resourceService.toResource(pElement);
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUser", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "updateProjectUser", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, pElement.getId()),
                                    MethodParamFactory.build(ProjectUser.class, pElement));
            resourceService.addLink(resource, this.getClass(), "removeProjectUser", LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUserList", LinkRels.LIST,
                                    MethodParamFactory.build(String.class, pElement.getStatus().toString()),
                                    MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class));
            // Specific links to add in WAITING_ACCESS state
            if (UserStatus.WAITING_ACCESS.equals(pElement.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "acceptAccessRequest", "accept",
                                        MethodParamFactory.build(Long.class, pElement.getId()));
                resourceService.addLink(resource, RegistrationController.class, "denyAccessRequest", "deny",
                                        MethodParamFactory.build(Long.class, pElement.getId()));
            }
            // Specific links to add in ACCESS_GRANTED state
            if (UserStatus.ACCESS_GRANTED.equals(pElement.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "inactiveAccess", "inactive",
                                        MethodParamFactory.build(Long.class, pElement.getId()));
            }
            // Specific links to add in ACCESS_DENIED state
            if (UserStatus.ACCESS_DENIED.equals(pElement.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "acceptAccessRequest", "accept",
                                        MethodParamFactory.build(Long.class, pElement.getId()));
            }
            // Specific links to add in ACCESS_INACTIVE state
            if (UserStatus.ACCESS_INACTIVE.equals(pElement.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "activeAccess", "active",
                                        MethodParamFactory.build(Long.class, pElement.getId()));
            }
        }
        return resource;
    }

    /**
     *
     * Special HATEOS resource maker for registered users asking for their own users. The toResource method is for
     * project admins.
     *
     * @param pProjectUser
     *            {@link ProjectUser} to transform to HATEOAS resources.
     * @return HATEOAS resources for {@link ProjectUser}
     * @since 1.0-SNAPSHOT
     */
    public Resource<ProjectUser> toResourceRegisteredUser(final ProjectUser pProjectUser) {
        Resource<ProjectUser> resource = resourceService.toResource(pProjectUser);
        if ((pProjectUser != null) && (pProjectUser.getId() != null)) {
            resource = resourceService.toResource(pProjectUser);
            resourceService.addLink(resource, this.getClass(), "retrieveCurrentProjectUser", LinkRels.SELF);
            resourceService.addLink(resource, this.getClass(), "updateCurrentProjectUser", LinkRels.UPDATE,
                                    MethodParamFactory.build(ProjectUser.class, pProjectUser));
        }
        return resource;
    }

}
