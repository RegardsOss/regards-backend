/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.user.AccessRequestDto;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserDto;
import fr.cnes.regards.modules.access.services.rest.user.utils.ComposableClientException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
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

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static fr.cnes.regards.modules.access.services.rest.user.utils.Try.handleClientFailure;
import static java.util.stream.Collectors.toList;

/**
 * Controller responsible for the /users(/*)? endpoints
 * @author svissier
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda

 */
@RestController
@RequestMapping(ProjectUsersController.TYPE_MAPPING)
public class ProjectUsersController implements IResourceController<ProjectUserDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUsersController.class);

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
    public static final String ACCESSRIGHTS_CLIENT = "accessrights-client";

    /**
     * Client handling project users
     */
    @Autowired
    private IProjectUsersClient projectUsersClient;

    @Autowired
    private IStorageRestClient storageClient;

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
     * Retrieve the {@link List} of all {@link ProjectUserDto}s.
     * @param status
     * @param emailStart
     * @param pageable
     * @param assembler
     * @return a {@link List} of {@link ProjectUserDto}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserDto>>> retrieveProjectUserList(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "partialEmail", required = false) String emailStart,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserDto> assembler
    ) throws ModuleException {
        return completeUserPagedResponseWithQuotas(
            () -> projectUsersClient.retrieveProjectUserList(status, emailStart, pageable.getPageNumber(), pageable.getPageSize()),
            pageable,
            assembler
        );
    }

    /**
     * Retrieve all users with a pending access requests.
     * @param pageable
     * @param assembler
     * @return The {@link List} of all {@link ProjectUserDto}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(value = PENDINGACCESSES, method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserDto>>> retrieveAccessRequestList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserDto> assembler
    ) throws ModuleException {
        return completeUserPagedResponseWithQuotas(
            () -> projectUsersClient.retrieveAccessRequestList(pageable.getPageNumber(), pageable.getPageSize()),
            pageable,
            assembler
        );
    }

    /**
     * Retrieve the {@link ProjectUserDto} of passed <code>id</code>.
     * @param userId The {@link ProjectUserDto}'s <code>id</code>
     * @return a {@link ProjectUserDto}
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUserDto>> retrieveProjectUser(@PathVariable("user_id") Long userId) throws ModuleException {
        return toResponse(
            Try.of(() -> projectUsersClient.retrieveProjectUser(userId))
                .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                .map(EntityModel::getContent)
                .flatMap(user ->
                    Try.of(() -> storageClient.getCurrentQuotas(user.getEmail()))
                        .transform(ignoreStorageQuotaErrors)
                        .map(limits -> new ProjectUserDto(
                            user,
                            limits
                        )))
                .mapError(ModuleException::new)
                .map(this::toResource)
                .map(resource -> new ResponseEntity<>(resource, HttpStatus.OK))
        );
    }

    /**
     * Retrieve the {@link ProjectUserDto} of current authenticated user
     * @return a {@link ProjectUserDto}
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the current authenticated project user and only display  metadata",
        role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProjectUserDto>> retrieveCurrentProjectUser() throws ModuleException {
        return combineProjectUserThenQuotaCalls(
            () -> projectUsersClient.retrieveCurrentProjectUser(),
            () -> storageClient.getCurrentQuotas(),
            this::toResource
        );
    }

    /**
     * Retrieve the {@link ProjectUserDto} of passed <code>id</code>.
     * @param userEmail The {@link ProjectUserDto}'s <code>id</code>
     * @return a {@link ProjectUserDto}
     */
    @ResponseBody
    @RequestMapping(value = "/email/{user_email}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUserDto>> retrieveProjectUserByEmail(@PathVariable("user_email") String userEmail)
        throws ModuleException
    {
        return combineProjectUserThenQuotaCalls(
            () -> projectUsersClient.retrieveProjectUserByEmail(userEmail),
            () -> storageClient.getCurrentQuotas(userEmail),
            this::toResource
        );
    }

    @ResponseBody
    @RequestMapping(value = "/email/{user_email}/admin", method = RequestMethod.GET)
    @ResourceAccess(description = "tell if user has role admin", role = DefaultRole.PUBLIC)
    public ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") String userEmail) {
        return projectUsersClient.isAdmin(userEmail);
    }

    /**
     * Update the {@link ProjectUserDto} of id <code>pUserId</code>.
     * @param userId The {@link ProjectUserDto} <code>id</code>
     * @param updatedProjectUser The new {@link ProjectUserDto}
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "update the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUserDto>> updateProjectUser(@PathVariable("user_id") Long userId,
                                                                         @RequestBody ProjectUserDto updatedProjectUser)
        throws ModuleException
    {
        String userEmail = updatedProjectUser.getEmail();

        Tuple2<ProjectUser, DownloadQuotaLimitsDto> t =
            makeProjectUserAndQuotaLimitsDto(updatedProjectUser);

        return combineQuotaThenProjectUserCalls(
            userEmail,
            () -> storageClient.upsertQuotaLimits(userEmail, t._2),
            () -> projectUsersClient.updateProjectUser(userId, t._1),
            this::toResource
        );
    }

    /**
     * Update the {@link ProjectUserDto} of current projet user authenticated.
     * @param updatedProjectUser The new {@link ProjectUserDto}
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the current authenticated project user", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProjectUserDto>> updateCurrentProjectUser(@RequestBody ProjectUserDto updatedProjectUser)
        throws ModuleException
    {
        String userEmail = authResolver.getUser();

        Tuple2<ProjectUser, DownloadQuotaLimitsDto> t =
            makeProjectUserAndQuotaLimitsDto(updatedProjectUser);

        return combineQuotaThenProjectUserCalls(
            userEmail,
            () -> storageClient.upsertQuotaLimits(userEmail, t._2),
            () -> projectUsersClient.updateCurrentProjectUser(t._1),
            this::toResourceRegisteredUser
        );
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
    public ResponseEntity<EntityModel<ProjectUserDto>> createUser(@Valid @RequestBody AccessRequestDto dto) throws ModuleException {
        String userEmail = dto.getEmail();

        fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto accessRequest =
            new fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto(
                userEmail,
                dto.getFirstName(),
                dto.getLastName(),
                dto.getRoleName(),
                dto.getMetadata(),
                dto.getPassword(),
                dto.getOriginUrl(),
                dto.getRequestLink()
            );

        DownloadQuotaLimitsDto limits =
            new DownloadQuotaLimitsDto(userEmail, dto.getMaxQuota(), dto.getRateLimit());

        return combineQuotaThenProjectUserCalls(
            userEmail,
            () -> storageClient.upsertQuotaLimits(userEmail, limits),
            () -> projectUsersClient.createUser(accessRequest),
            this::toResourceRegisteredUser
        );
    }

    /**
     * Delete the {@link ProjectUserDto} of passed <code>id</code>.
     * @param userId The {@link ProjectUserDto}'s <code>id</code>
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = USER_ID_RELATIVE_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId) {
        return projectUsersClient.removeProjectUser(userId);
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUserDto} for the role of passed
     * <code>id</code> by crawling through parents' hierarachy.
     * @param roleId The role's <code>id</code>
     * @param pageable
     * @param assembler
     * @return The {@link List} of {@link ProjectUserDto} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = ROLES_ROLE_ID, method = RequestMethod.GET)
    @ResourceAccess(
        description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_id",
        role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserDto>>> retrieveRoleProjectUserList(
        @PathVariable("role_id") Long roleId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserDto> assembler
    ) throws ModuleException {
        return completeUserPagedResponseWithQuotas(
            () -> projectUsersClient.retrieveRoleProjectUserList(roleId, pageable.getPageNumber(), pageable.getPageSize()),
            pageable,
            assembler
        );
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUserDto} for the role of passed
     * <code>name</code> by crawling through parents' hierarachy.
     * @param role The role's <code>name</code>
     * @param pageable
     * @param assembler
     * @return The {@link List} of {@link ProjectUserDto} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @ResourceAccess(
        description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_name",
        role = DefaultRole.ADMIN)
    @RequestMapping(value = "/roles", method = RequestMethod.GET)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserDto>>> retrieveRoleProjectUsersList(
        @RequestParam("role_name") String role,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserDto> assembler
    ) throws ModuleException {
        return completeUserPagedResponseWithQuotas(
            () -> projectUsersClient.retrieveRoleProjectUsersList(role, pageable.getPageNumber(), pageable.getPageSize()),
            pageable,
            assembler
        );
    }

    private ResponseEntity<PagedModel<EntityModel<ProjectUserDto>>> completeUserPagedResponseWithQuotas(
        Supplier<ResponseEntity<PagedModel<EntityModel<ProjectUser>>>> usersRequest,
        Pageable pageable,
        PagedResourcesAssembler<ProjectUserDto> pagedResourcesAssembler
    ) throws ModuleException {
        AtomicReference<PagedModel.PageMetadata> meta = new AtomicReference<>();
        AtomicReference<io.vavr.collection.List<ProjectUser>> users = new AtomicReference<>(io.vavr.collection.List.empty());
        return toResponse(
            Try.ofSupplier(usersRequest)
                .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                .peek(r -> meta.set(r.getMetadata())) // need a piece of state (pagination metadata) for later if success
                .map(PagedModel::getContent)
                .map(c -> c.stream()
                    .map(EntityModel::getContent)
                    // fill the list of users while mapping, we'll need'em later
                    .peek(u -> users.updateAndGet(l -> l.append(u)))
                    .map(ProjectUser::getEmail)
                    .toArray(String[]::new))
                .flatMap(a ->
                    Try.of(() -> storageClient.getCurrentQuotasList(a))
                        .map(ResponseEntity::getBody)
                        // special value for frontend if any error on storage or storage not deploy
                        .onFailure(t -> LOGGER.debug("Failed to query rs-storage for quotas.", t))
                        .orElse(() -> Try.success(Arrays.stream(a).map(email -> new UserCurrentQuotas(email, null, null, null, null)).collect(toList())))
                        .toValidation(ComposableClientException::make)
                )
                .map(quotas -> users.get()
                    .zip(quotas)
                    .map(ul -> new ProjectUserDto(
                        ul._1,
                        ul._2
                    ))
                    .toJavaList()
                )
                .map(list -> new PageImpl<>(list, pageable, meta.get().getTotalElements()))
                .map(page -> toPagedResources(page, pagedResourcesAssembler))
                .map(paged -> new ResponseEntity<>(paged, HttpStatus.OK))
                .mapError(ModuleException::new)
        );
    }

    private Tuple2<ProjectUser, DownloadQuotaLimitsDto> makeProjectUserAndQuotaLimitsDto(ProjectUserDto updatedProjectUser) {
        String userEmail = updatedProjectUser.getEmail();

        ProjectUser user = new ProjectUser();
        user.setId(updatedProjectUser.getId());
        user.setEmail(userEmail);
        user.setLastConnection(updatedProjectUser.getLastConnection());
        user.setLastUpdate(updatedProjectUser.getLastUpdate());
        user.setStatus(updatedProjectUser.getStatus());
        user.setMetadata(updatedProjectUser.getMetadata());
        user.setRole(updatedProjectUser.getRole());
        user.setPermissions(updatedProjectUser.getPermissions());
        user.setLicenseAccepted(updatedProjectUser.isLicenseAccepted());

        DownloadQuotaLimitsDto limits =
            new DownloadQuotaLimitsDto(userEmail, updatedProjectUser.getMaxQuota(), updatedProjectUser.getRateLimit());

        return Tuple.of(user, limits);
    }

    private ResponseEntity<EntityModel<ProjectUserDto>> combineProjectUserThenQuotaCalls(
        Supplier<ResponseEntity<EntityModel<ProjectUser>>> projectUsersCall,
        Supplier<ResponseEntity<UserCurrentQuotas>> quotaCall,
        Function<ProjectUserDto, EntityModel<ProjectUserDto>> resourceMapper
    ) throws ModuleException {
        return toResponse(
            Try.ofSupplier(projectUsersCall)
                .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                .map(EntityModel::getContent)
                .combine(Try.ofSupplier(quotaCall)
                    .transform(ignoreStorageQuotaErrors))
                .ap(ProjectUserDto::new)
                .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                .map(resourceMapper)
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
        );
    }

    private ResponseEntity<EntityModel<ProjectUserDto>> combineQuotaThenProjectUserCalls(
        String userEmail,
        Supplier<ResponseEntity<DownloadQuotaLimitsDto>> quotaLimitsCall,
        Supplier<ResponseEntity<EntityModel<ProjectUser>>> projectUsersCall,
        Function<ProjectUserDto, EntityModel<ProjectUserDto>> resourceMapper
    ) throws ModuleException {
        return toResponse(
            Try.ofSupplier(quotaLimitsCall)
                .map(unit -> storageClient.getCurrentQuotas(userEmail))
                .transform(ignoreStorageQuotaErrors)
                .combine(Try.ofSupplier(projectUsersCall)
                    .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                    .map(EntityModel::getContent))
                .ap(ProjectUserDto::new)
                .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                .map(resourceMapper)
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
        );
    }

    private final Function<Try<ResponseEntity<UserCurrentQuotas>>, Validation<ComposableClientException, UserCurrentQuotas>> ignoreStorageQuotaErrors =
        t -> t
            .map(ResponseEntity::getBody)
            // special value for frontend if any error on storage or storage not deploy
            .onFailure(e -> LOGGER.debug("Failed to query rs-storage for quotas.", e))
            .orElse(() -> Try.success(new UserCurrentQuotas(null, null, null, null, null)))
            .toValidation(ComposableClientException::make);

    private <V> V toResponse(
        Validation<ModuleException, V> v
    ) throws ModuleException {
        if (v.isValid()) {
            return v.get();
        } else {
            throw v.getError();
        }
    }

    @Override
    public EntityModel<ProjectUserDto> toResource(final ProjectUserDto element, final Object... extras) {
        EntityModel<ProjectUserDto> resource = resourceService.toResource(element);
        if ((element != null) && (element.getId() != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource, this.getClass(), "retrieveProjectUser", LinkRels.SELF,
                MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "updateProjectUser", LinkRels.UPDATE,
                MethodParamFactory.build(Long.class, element.getId()),
                MethodParamFactory.build(ProjectUserDto.class, element));
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

    /**
     * Special HATEOS resource maker for registered users asking for their own users. The toResource method is for
     * project admins.
     * @param projectUser {@link ProjectUserDto} to transform to HATEOAS resources.
     * @return HATEOAS resources for {@link ProjectUserDto}

     */
    public EntityModel<ProjectUserDto> toResourceRegisteredUser(ProjectUserDto projectUser) {
        EntityModel<ProjectUserDto> resource = resourceService.toResource(projectUser);
        if ((projectUser != null) && (projectUser.getId() != null)) {
            resource = resourceService.toResource(projectUser);
            resourceService.addLink(resource, this.getClass(), "retrieveCurrentProjectUser", LinkRels.SELF);
            resourceService.addLink(resource, this.getClass(), "updateCurrentProjectUser", LinkRels.UPDATE,
                MethodParamFactory.build(ProjectUserDto.class, projectUser));
        }
        return resource;
    }
}
