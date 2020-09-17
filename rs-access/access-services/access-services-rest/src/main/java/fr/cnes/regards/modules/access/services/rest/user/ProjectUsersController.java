/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller responsible for the /users(/*)? endpoints
 * @author svissier
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda

 */
@RestController
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

    public static final String ROLES_ROLE_ID = "/roles/{role_id}";

    public static final String PENDINGACCESSES = "/pendingaccesses";

    /**
     * Client handling project users
     */
    @Autowired
    private IProjectUsersClient projectUsersClient;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     * @param status
     * @param emailStart
     * @param pageable
     * @param pagedResourcesAssembler
     * @return a {@link List} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "partialEmail", required = false) String emailStart,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ProjectUser> pagedResourcesAssembler)
    {
        ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response =
            projectUsersClient.retrieveProjectUserList(status, emailStart, pageable.getPageNumber(), pageable.getPageSize());
        return response.getStatusCode().is2xxSuccessful()
            ? new ResponseEntity<>(
                toPagedResources(
                    new PageImpl<>(
                        response.getBody().getContent().stream().map(EntityModel::getContent).collect(Collectors.toList()),
                        pageable,
                        response.getBody().getMetadata().getTotalElements()
                    ),
                    pagedResourcesAssembler)
                ,
                response.getHeaders(),
                response.getStatusCode())
            : response;
    }

    /**
     * Retrieve all users with a pending access requests.
     * @param pageable
     * @param assembler
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(value = PENDINGACCESSES, method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ProjectUser> assembler) {
        ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response =
            projectUsersClient.retrieveAccessRequestList(pageable.getPageNumber(), pageable.getPageSize());
        return response.getStatusCode().is2xxSuccessful()
            ? new ResponseEntity<>(
            toPagedResources(
                new PageImpl<>(
                    response.getBody().getContent().stream().map(EntityModel::getContent).collect(Collectors.toList()),
                    pageable,
                    response.getBody().getMetadata().getTotalElements()
                ),
                assembler)
            ,
            response.getHeaders(),
            response.getStatusCode())
            : response;
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     * @param userId The {@link ProjectUser}'s <code>id</code>
     * @return a {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long userId) {
        return projectUsersClient.retrieveProjectUser(userId);
    }

    /**
     * Retrieve the {@link ProjectUser} of current authenticated user
     * @return a {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the current authenticated project user and only display  metadata",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser() {
        return projectUsersClient.retrieveCurrentProjectUser();
    }

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     * @param userEmail The {@link ProjectUser}'s <code>id</code>
     * @return a {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/email/{user_email}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(@PathVariable("user_email") String userEmail) {
        return projectUsersClient.retrieveProjectUserByEmail(userEmail);
    }

    @ResponseBody
    @RequestMapping(value = "/email/{user_email}/admin", method = RequestMethod.GET)
    @ResourceAccess(description = "tell if user has role admin", role = DefaultRole.PUBLIC)
    public ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") String userEmail) {
        return projectUsersClient.isAdmin(userEmail);
    }

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     * @param userId The {@link ProjectUser} <code>id</code>
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "update the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long userId,
                                                                      @RequestBody ProjectUser updatedProjectUser) {
        return projectUsersClient.updateProjectUser(userId, updatedProjectUser);
    }

    /**
     * Update the {@link ProjectUser} of current projet user authenticated.
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the current authenticated project user", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(@RequestBody ProjectUser updatedProjectUser) {
        return projectUsersClient.updateCurrentProjectUser(updatedProjectUser);
    }

    /**
     * Create a new user by bypassing registration process (accounts and projectUser validation)
     * @param dto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a projectUser by bypassing registration process (Administrator feature)",
            role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid @RequestBody AccessRequestDto dto) {
        return projectUsersClient.createUser(dto);
    }

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     * @param userId The {@link ProjectUser}'s <code>id</code>
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId) {
        return projectUsersClient.removeProjectUser(userId);
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUser} for the role of passed
     * <code>id</code> by crawling through parents' hierarachy.
     * @param roleId The role's <code>id</code>
     * @param pageable
     * @param assembler
     * @return The {@link List} of {@link ProjectUser} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = ROLES_ROLE_ID, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_id",
            role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(
        @PathVariable("role_id") Long roleId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUser> assembler
    ) {
        ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response =
            projectUsersClient.retrieveRoleProjectUserList(roleId, pageable.getPageNumber(), pageable.getPageSize());
        return response.getStatusCode().is2xxSuccessful()
            ? new ResponseEntity<>(
            toPagedResources(
                new PageImpl<>(
                    response.getBody().getContent().stream().map(EntityModel::getContent).collect(Collectors.toList()),
                    pageable,
                    response.getBody().getMetadata().getTotalElements()
                ),
                assembler)
            ,
            response.getHeaders(),
            response.getStatusCode())
            : response;
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUser} for the role of passed
     * <code>name</code> by crawling through parents' hierarachy.
     * @param role The role's <code>name</code>
     * @param pageable
     * @param assembler
     * @return The {@link List} of {@link ProjectUser} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @ResourceAccess(
            description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_name",
            role = DefaultRole.ADMIN)
    @RequestMapping(value = "/roles", method = RequestMethod.GET)
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(
            @RequestParam("role_name") String role,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ProjectUser> assembler
    ) {
        ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response =
            projectUsersClient.retrieveRoleProjectUsersList(role, pageable.getPageNumber(), pageable.getPageSize());
        return response.getStatusCode().is2xxSuccessful()
            ? new ResponseEntity<>(
            toPagedResources(
                new PageImpl<>(
                    response.getBody().getContent().stream().map(EntityModel::getContent).collect(Collectors.toList()),
                    pageable,
                    response.getBody().getMetadata().getTotalElements()
                ),
                assembler)
            ,
            response.getHeaders(),
            response.getStatusCode())
            : response;
    }

    @Override
    public EntityModel<ProjectUser> toResource(final ProjectUser element, final Object... extras) {
        EntityModel<ProjectUser> resource = resourceService.toResource(element);
        if ((element != null) && (element.getId() != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUser", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "updateProjectUser", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(ProjectUser.class, element));
            resourceService.addLink(resource, this.getClass(), "removeProjectUser", LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUserList", LinkRels.LIST,
                                    MethodParamFactory.build(String.class, element.getStatus().toString()),
                                    MethodParamFactory.build(String.class), MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class));
            // Specific links to add in WAITING_ACCESS state
            if (UserStatus.WAITING_ACCESS.equals(element.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "acceptAccessRequest",
                                        LinkRelation.of("accept"),
                                        MethodParamFactory.build(Long.class, element.getId()));
                resourceService.addLink(resource, RegistrationController.class, "denyAccessRequest",
                                        LinkRelation.of("deny"), MethodParamFactory.build(Long.class, element.getId()));
            }
            // Specific links to add in ACCESS_GRANTED state
            if (UserStatus.ACCESS_GRANTED.equals(element.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "inactiveAccess",
                                        LinkRelation.of("inactive"),
                                        MethodParamFactory.build(Long.class, element.getId()));
            }
            // Specific links to add in ACCESS_DENIED state
            if (UserStatus.ACCESS_DENIED.equals(element.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "acceptAccessRequest",
                                        LinkRelation.of("accept"),
                                        MethodParamFactory.build(Long.class, element.getId()));
            }
            // Specific links to add in ACCESS_INACTIVE state
            if (UserStatus.ACCESS_INACTIVE.equals(element.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "activeAccess",
                                        LinkRelation.of("active"),
                                        MethodParamFactory.build(Long.class, element.getId()));
            }
        }
        return resource;
    }

//    /**
//     * Special HATEOS resource maker for registered users asking for their own users. The toResource method is for
//     * project admins.
//     * @param projectUser {@link ProjectUser} to transform to HATEOAS resources.
//     * @return HATEOAS resources for {@link ProjectUser}
//
//     */
//    public EntityModel<ProjectUser> toResourceRegisteredUser(ProjectUser projectUser) {
//        EntityModel<ProjectUser> resource = resourceService.toResource(projectUser);
//        if ((projectUser != null) && (projectUser.getId() != null)) {
//            resource = resourceService.toResource(projectUser);
//            resourceService.addLink(resource, this.getClass(), "retrieveCurrentProjectUser", LinkRels.SELF);
//            resourceService.addLink(resource, this.getClass(), "updateCurrentProjectUser", LinkRels.UPDATE,
//                                    MethodParamFactory.build(ProjectUser.class, projectUser));
//        }
//        return resource;
//    }

}
