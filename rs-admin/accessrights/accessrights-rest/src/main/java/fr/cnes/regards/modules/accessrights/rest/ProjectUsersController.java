/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserExportService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserGroupService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(ProjectUsersController.TYPE_MAPPING)
public class ProjectUsersController implements IResourceController<ProjectUser> {

    public static final String TYPE_MAPPING = "/users";

    public static final String USER_ID_RELATIVE_PATH = "/{user_id}";

    public static final String ROLES = "/roles";

    public static final String ROLES_ROLE_ID = ROLES + "/{role_id}";

    public static final String PENDING_ACCESSES = "/pendingaccesses";

    public static final String MY_USER = "/myuser";

    public static final String EMAIL = "/email/{email}";

    public static final String EMAIL_ADMIN = EMAIL + "/admin";

    public static final String EMAIL_GROUPS = EMAIL + "/groups";

    public static final String EMAIL_ORIGIN = EMAIL + "/origin/{origin}";

    public static final String EMAIL_VERIFICATION_SEND = EMAIL + "/verification/resend";

    public static final String EXPORT = "/export";

    public static final String COUNT_BY_ACCESS_GROUP = "/count";

    public static final String SEARCH_USERS = "/search";

    private final IProjectUserService projectUserService;

    private final ProjectUserWorkflowManager projectUserWorkflowManager;

    private final IResourceService resourceService;

    private final IRoleService roleService;

    private final IAuthenticationResolver authResolver;

    private final ProjectUserExportService projectUserExportService;

    private final ProjectUserGroupService projectUserGroupService;

    public ProjectUsersController(IProjectUserService projectUserService,
                                  ProjectUserWorkflowManager projectUserWorkflowManager,
                                  IResourceService resourceService,
                                  IRoleService roleService,
                                  IAuthenticationResolver authResolver,
                                  ProjectUserExportService projectUserExportService,
                                  ProjectUserGroupService projectUserGroupService) {
        this.projectUserService = projectUserService;
        this.projectUserWorkflowManager = projectUserWorkflowManager;
        this.resourceService = resourceService;
        this.roleService = roleService;
        this.authResolver = authResolver;
        this.projectUserExportService = projectUserExportService;
        this.projectUserGroupService = projectUserGroupService;
    }

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @param parameters              search parameters as request params
     * @param pageable                paging parameters
     * @param pagedResourcesAssembler assembler
     * @return a {@link List} of {@link ProjectUser}
     */
    @PostMapping(SEARCH_USERS)
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(
        @RequestBody ProjectUserSearchParameters parameters,
        @PageableDefault(sort = "created", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUser> pagedResourcesAssembler) {
        return ResponseEntity.ok(toPagedResources(projectUserService.retrieveUserList(parameters, pageable),
                                                  pagedResourcesAssembler));
    }

    /**
     * Retrieve all users with a pending access request.
     *
     * @param pageable  paging parameters
     * @param assembler assembler
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @GetMapping(PENDING_ACCESSES)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(
        @PageableDefault(sort = "created", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUser> assembler) {
        return ResponseEntity.ok(toPagedResources(projectUserService.retrieveAccessRequestList(pageable), assembler));
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param userId The {@link ProjectUser}'s <code>id</code>
     * @return a {@link ProjectUser}
     */
    @GetMapping(USER_ID_RELATIVE_PATH)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long userId)
        throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(projectUserService.retrieveUser(userId)));
    }

    /**
     * Retrieve the {@link ProjectUser} of current authenticated user
     *
     * @return a {@link ProjectUser}
     */
    @GetMapping(MY_USER)
    @ResourceAccess(description = "retrieve the current authenticated project user and only display  metadata",
        role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser()
        throws EntityNotFoundException, EntityOperationForbiddenException {
        String curentUserEmail = authResolver.getUser();
        if ((curentUserEmail == null) || curentUserEmail.isEmpty()) {
            throw new EntityOperationForbiddenException("Unable to retrieve current authenticated user.");
        }
        return ResponseEntity.ok(toResource(projectUserService.retrieveOneByEmail(curentUserEmail)));
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param userEmail The {@link ProjectUser}'s <code>id</code>
     * @return a {@link ProjectUser}
     */
    @GetMapping(EMAIL)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(@PathVariable("email") String userEmail)
        throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(projectUserService.retrieveOneByEmail(userEmail)));
    }

    @GetMapping(EMAIL_ADMIN)
    @ResourceAccess(description = "tell if user has role admin", role = DefaultRole.PUBLIC)
    public ResponseEntity<Boolean> isAdmin(@PathVariable("email") String userEmail) throws EntityNotFoundException {
        ProjectUser user = projectUserService.retrieveOneByEmail(userEmail);
        boolean isAdmin = user.getRole().getName().equals(DefaultRole.INSTANCE_ADMIN.toString());
        isAdmin |= user.getRole().getName().equals(DefaultRole.ADMIN.toString());
        isAdmin |= user.getRole().getName().equals(DefaultRole.PROJECT_ADMIN.toString());
        isAdmin |= user.getRole().getParentRole() != null && user.getRole()
                                                                 .getParentRole()
                                                                 .getName()
                                                                 .equals(DefaultRole.ADMIN.toString());
        return ResponseEntity.ok(isAdmin);
    }

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param userId             The {@link ProjectUser} <code>id</code>
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> is different from the
     *                         id of <code>updatedProjectUser</code><br>
     *                         {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *                         be found<br>
     */
    @PutMapping(USER_ID_RELATIVE_PATH)
    @ResourceAccess(description = "update the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long userId,
                                                                      @RequestBody ProjectUser updatedProjectUser)
        throws EntityException {
        return ResponseEntity.ok(toResource(projectUserService.updateUserInfos(userId, updatedProjectUser)));
    }

    /**
     * Update the {@link ProjectUser} of current projet user authenticated.
     *
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> is different from the
     *                         id of <code>updatedProjectUser</code><br>
     *                         {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *                         be found<br>
     *                         {@link EntityOperationForbiddenException} Thrown when the user to update is not the current
     *                         authenticated user<br>
     */
    @PutMapping(MY_USER)
    @ResourceAccess(description = "Update the current authenticated project user", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(
        @RequestBody ProjectUser updatedProjectUser) throws EntityException {
        ProjectUser user = projectUserService.retrieveCurrentUser();
        if (!user.getId().equals(updatedProjectUser.getId())) {
            throw new EntityOperationForbiddenException("You are only allowed to update your own user properties.");
        }
        return ResponseEntity.ok(toResourceRegisteredUser(projectUserService.updateUserInfos(user.getId(),
                                                                                             updatedProjectUser)));
    }

    /**
     * Create a new user by bypassing registration process (accounts and projectUser validation)
     *
     * @param dto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException if error occurs.
     */
    @PostMapping
    @ResourceAccess(description = "Create a projectUser by bypassing registration process (Administrator feature)",
        role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid @RequestBody AccessRequestDto dto)
        throws EntityException {
        ProjectUser userCreated = projectUserService.createProjectUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResource(userCreated));
    }

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param userId The {@link ProjectUser}'s <code>id</code>
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} when the project user has a <code>status</code> not
     *                         allowing removal<br>
     *                         {@link EntityNotFoundException} user not found<br>
     */
    @DeleteMapping(USER_ID_RELATIVE_PATH)
    @ResourceAccess(description = "remove the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId) throws EntityException {
        ProjectUser projectUser = projectUserService.retrieveUser(userId);
        if (projectUserService.canDelete(projectUser)) {
            projectUserWorkflowManager.removeAccess(projectUser);
        } else {
            throw new EntityOperationForbiddenException(String.format(
                "You cannot delete %s because he has more privilege than you",
                projectUser.getEmail()));
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUser} for the {@link Role} of passed
     * <code>id</code> by crawling through parents' hierarachy.
     *
     * @param roleId    The {@link Role}'s <code>id</code>
     * @param pageable  paging parameters
     * @param assembler assembler
     * @return The {@link List} of {@link ProjectUser} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @GetMapping(ROLES_ROLE_ID)
    @ResourceAccess(
        description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_id",
        role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(
        @PathVariable("role_id") Long roleId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUser> assembler) throws EntityNotFoundException {
        Page<ProjectUser> projectUserList = roleService.retrieveRoleProjectUserList(roleId, pageable);
        return ResponseEntity.ok(toPagedResources(projectUserList, assembler));
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUser} for the {@link Role} of passed
     * <code>name</code> by crawling through parents' hierarachy.
     *
     * @param role      The {@link Role}'s <code>name</code>
     * @param pageable  paging parameters
     * @param assembler assembler
     * @return The {@link List} of {@link ProjectUser} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @GetMapping(ROLES)
    @ResourceAccess(
        description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_name",
        role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(
        @RequestParam("role_name") String role,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUser> assembler) throws EntityNotFoundException {
        Page<ProjectUser> projectUserList = roleService.retrieveRoleProjectUserList(role, pageable);
        return ResponseEntity.ok(toPagedResources(projectUserList, assembler));
    }

    @PostMapping(EMAIL_GROUPS)
    @ResourceAccess(description = "Link access groups to a project user identified by email",
        role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> linkAccessGroups(@PathVariable("email") String email, @RequestBody List<String> groups)
        throws EntityNotFoundException {
        projectUserGroupService.linkAccessGroups(email, groups);
        return ResponseEntity.ok().build();
    }

    @PutMapping(EMAIL_ORIGIN)
    @ResourceAccess(description = "Update the origin of a project user identified by email",
        role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> updateOrigin(@PathVariable("email") String email, @PathVariable("origin") String origin)
        throws EntityNotFoundException {
        projectUserService.updateOrigin(email, origin);
        return ResponseEntity.ok().build();
    }

    @GetMapping(EMAIL_VERIFICATION_SEND)
    @ResourceAccess(description = "Send a new verification email for a user creation", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> sendVerificationEmail(@PathVariable("email") String email)
        throws EntityNotFoundException {
        projectUserService.sendVerificationEmail(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = EXPORT, produces = "text/csv")
    @ResourceAccess(description = "Generate a CSV file with all project users", role = DefaultRole.EXPLOIT)
    public void exportAsCSV(ProjectUserSearchParameters parameters, HttpServletResponse response) throws IOException {
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=users.csv");
        response.setContentType("text/csv");
        projectUserExportService.export(new BufferedWriter(response.getWriter()), parameters);
    }

    @GetMapping(COUNT_BY_ACCESS_GROUP)
    @ResourceAccess(description = "Count users by access group", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Map<String, Long>> getUserCountByAccessGroup() {
        return ResponseEntity.ok(projectUserService.getUserCountByAccessGroup());
    }

    @Override
    public EntityModel<ProjectUser> toResource(final ProjectUser element, final Object... extras) {
        EntityModel<ProjectUser> resource = resourceService.toResource(element);
        if ((element != null) && (element.getId() != null)) {
            resource = resourceService.toResource(element);
            MethodParam<Long> idParam = MethodParamFactory.build(Long.class, element.getId());
            Class<? extends ProjectUsersController> clazz = this.getClass();
            resourceService.addLink(resource, clazz, "retrieveProjectUser", LinkRels.SELF, idParam);
            resourceService.addLink(resource,
                                    clazz,
                                    "updateProjectUser",
                                    LinkRels.UPDATE,
                                    idParam,
                                    MethodParamFactory.build(ProjectUser.class, element));
            if (projectUserService.canDelete(element)) {
                resourceService.addLink(resource, clazz, "removeProjectUser", LinkRels.DELETE, idParam);
            }
            resourceService.addLink(resource,
                                    clazz,
                                    "retrieveProjectUserList",
                                    LinkRels.LIST,
                                    MethodParamFactory.build(ProjectUserSearchParameters.class),
                                    MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class));
            // Specific links to add in WAITING_ACCESS state
            if (UserStatus.WAITING_ACCESS.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "acceptAccessRequest",
                                        LinkRelation.of("accept"),
                                        idParam);
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "denyAccessRequest",
                                        LinkRelation.of("deny"),
                                        idParam);
            }
            // Specific links to add in ACCESS_GRANTED state
            if (UserStatus.ACCESS_GRANTED.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "inactiveAccess",
                                        LinkRelation.of("inactive"),
                                        idParam);
            }
            // Specific links to add in ACCESS_DENIED state
            if (UserStatus.ACCESS_DENIED.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "acceptAccessRequest",
                                        LinkRelation.of("accept"),
                                        idParam);
            }
            // Specific links to add in ACCESS_INACTIVE state
            if (UserStatus.ACCESS_INACTIVE.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "activeAccess",
                                        LinkRelation.of("active"),
                                        idParam);
            }
            if (UserStatus.WAITING_EMAIL_VERIFICATION.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        clazz,
                                        "sendVerificationEmail",
                                        LinkRelation.of("sendVerificationEmail"),
                                        MethodParamFactory.build(String.class));
            }
        }
        return resource;
    }

    /**
     * Special HATEOAS resource maker for registered users asking for their own users. The toResource method is for project admins.
     *
     * @param projectUser {@link ProjectUser} to transform to HATEOAS resources.
     * @return HATEOAS resources for {@link ProjectUser}
     */
    public EntityModel<ProjectUser> toResourceRegisteredUser(ProjectUser projectUser) {
        EntityModel<ProjectUser> resource = resourceService.toResource(projectUser);
        if ((projectUser != null) && (projectUser.getId() != null)) {
            resource = resourceService.toResource(projectUser);
            resourceService.addLink(resource, this.getClass(), "retrieveCurrentProjectUser", LinkRels.SELF);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "updateCurrentProjectUser",
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(ProjectUser.class, projectUser));
        }
        return resource;
    }

}
