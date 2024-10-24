/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Preconditions;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.swagger.autoconfigure.PageableQueryParam;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserCreateDto;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserReadDto;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserUpdateDto;
import fr.cnes.regards.modules.access.services.rest.user.utils.ComposableClientException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.configuration.service.SearchHistoryService;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.fileaccess.dto.quota.UserCurrentQuotasDto;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static fr.cnes.regards.modules.access.services.rest.user.utils.Try.handleClientFailure;
import static java.util.stream.Collectors.toList;

/**
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Tag(name = "Project users controller")
@RestController
@RequestMapping(ProjectUsersController.TYPE_MAPPING)
public class ProjectUsersController implements IResourceController<ProjectUserReadDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUsersController.class);

    public static final String TYPE_MAPPING = "/users";

    public static final String SEARCH_USERS_PATH = "/search";

    public static final String USER_ID_RELATIVE_PATH = "/{user_id}";

    public static final String ROLES_ROLE_ID = "/roles/{role_id}";

    public static final String PENDINGACCESSES = "/pendingaccesses";

    public static final String ACCESSRIGHTS_CLIENT = "accessrights-client";

    public static final String STORAGE_CLIENT = "storage-client";

    private final IProjectUsersClient projectUsersClient;

    private final IStorageRestClient storageClient;

    private final IResourceService resourceService;

    private final IAuthenticationResolver authenticationResolver;

    private final IRolesClient rolesClient;

    private final SearchHistoryService searchHistoryService;

    private final Function<Try<ResponseEntity<UserCurrentQuotasDto>>, Validation<ComposableClientException, UserCurrentQuotasDto>> ignoreStorageQuotaErrors = t -> t.map(
                                                                                                                                                                        ResponseEntity::getBody)
                                                                                                                                                                    // special value for frontend if any error on storage or storage not deploy
                                                                                                                                                                    .onFailure(
                                                                                                                                                                        e -> LOGGER.debug(
                                                                                                                                                                            "Failed to query rs-storage for quotas.",
                                                                                                                                                                            e))
                                                                                                                                                                    .orElse(
                                                                                                                                                                        () -> Try.success(
                                                                                                                                                                            new UserCurrentQuotasDto()))
                                                                                                                                                                    .toValidation(
                                                                                                                                                                        ComposableClientException::make);

    @Value("${spring.application.name}")
    private String appName;

    public ProjectUsersController(IProjectUsersClient projectUsersClient,
                                  IStorageRestClient storageClient,
                                  IResourceService resourceService,
                                  IAuthenticationResolver authenticationResolver,
                                  IRolesClient rolesClient,
                                  SearchHistoryService searchHistoryService) {
        this.projectUsersClient = projectUsersClient;
        this.storageClient = storageClient;
        this.resourceService = resourceService;
        this.authenticationResolver = authenticationResolver;
        this.rolesClient = rolesClient;
        this.searchHistoryService = searchHistoryService;
    }

    /**
     * Retrieve the {@link List} of all {@link ProjectUserReadDto}s.
     *
     * @param pageable  paging info
     * @param assembler assembler info
     * @param filters   search parameters
     * @return a {@link List} of {@link ProjectUserReadDto}
     */
    @PostMapping(value = SEARCH_USERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get users of the project",
               description = "Return a page of users of the project matching criterias.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All users of the project were retrieved.") })
    @ResourceAccess(description = "EndPoint to retrieve all users of the project matching criterias",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveProjectUserList(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterias.",
                                                              content = @Content(schema = @Schema(implementation = SearchProjectUserParameters.class)))
        @Parameter(description = "Filter criterias for users of the project") @RequestBody
        SearchProjectUserParameters filters,
        @PageableQueryParam @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {

        return completeUserPagedResponseWithQuotas(() -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.retrieveProjectUserList(filters, pageable);
        }, pageable, assembler);
    }

    /**
     * Retrieve all users with a pending access request.
     *
     * @param pageable  paging info
     * @param assembler assembler info
     * @return The {@link List} of all {@link ProjectUserReadDto}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @GetMapping(value = PENDINGACCESSES, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieves the list of users with a pending access request",
                    role = DefaultRole.PROJECT_ADMIN)
    @Operation(summary = "Get a list of users",
               description = "Retrieve a page containing a list of users with a pending access request")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All users of the project with a pending access request were retrieved.") })
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveAccessRequestList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {
        return completeUserPagedResponseWithQuotas(() -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.retrieveAccessRequestList(pageable);
        }, pageable, assembler);
    }

    /**
     * Retrieve the {@link ProjectUserReadDto} of passed <code>id</code>.
     *
     * @param userId The {@link ProjectUserReadDto}'s <code>id</code>
     * @return a {@link ProjectUserReadDto}
     */
    @GetMapping(value = USER_ID_RELATIVE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieve the project user metadata by its identifier",
                    role = DefaultRole.EXPLOIT)
    @Operation(summary = "Get a project user", description = "Retrieve the project user metadata by its identifier")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "The user metadata were retrieved.") })
    public ResponseEntity<EntityModel<ProjectUserReadDto>> retrieveProjectUser(@PathVariable("user_id") Long userId)
        throws ModuleException {
        return toResponse(Try.of(() -> projectUsersClient.retrieveProjectUser(userId))
                             .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                             .map(EntityModel::getContent)
                             .flatMap(user -> Try.run(() -> FeignSecurityManager.asUser(authenticationResolver.getUser(),
                                                                                        RoleAuthority.getSysRole(appName)))
                                                 .map(unused -> storageClient.getCurrentQuotas(user.getEmail()))
                                                 .andFinally(FeignSecurityManager::reset)
                                                 .transform(ignoreStorageQuotaErrors)
                                                 .map(limits -> new ProjectUserReadDto(user, limits)))
                             .mapError(ModuleException::new)
                             .map(this::toResource)
                             .map(resource -> new ResponseEntity<>(resource, HttpStatus.OK)));
    }

    /**
     * Retrieve the {@link ProjectUserReadDto} of current authenticated user
     *
     * @return a {@link ProjectUserReadDto}
     */
    @GetMapping(value = "/myuser", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieve the current authenticated project user metadata",
                    role = DefaultRole.REGISTERED_USER)
    @Operation(summary = "Get the current project user metadata",
               description = "Retrieve the current authenticated project user metadata")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Returns the current authenticated project user") })
    public ResponseEntity<EntityModel<ProjectUserReadDto>> retrieveCurrentProjectUser() throws ModuleException {
        return combineProjectUserThenQuotaCalls(projectUsersClient::retrieveCurrentProjectUser,
                                                storageClient::getCurrentQuotas,
                                                this::toResource);
    }

    /**
     * Retrieve the {@link ProjectUserReadDto} of passed <code>id</code>.
     *
     * @param userEmail The {@link ProjectUserReadDto}'s <code>id</code>
     * @return a {@link ProjectUserReadDto}
     */
    @GetMapping(value = "/email/{user_email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieve the project user metadata by its email",
                    role = DefaultRole.EXPLOIT)
    @Operation(summary = "Get the project user", description = "Retrieve the project user metadata by its emaila")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns the project user") })
    public ResponseEntity<EntityModel<ProjectUserReadDto>> retrieveProjectUserByEmail(
        @PathVariable("user_email") String userEmail) throws ModuleException {
        return combineProjectUserThenQuotaCalls(() -> projectUsersClient.retrieveProjectUserByEmail(userEmail), () -> {
            FeignSecurityManager.asUser(authenticationResolver.getUser(), RoleAuthority.getSysRole(appName));
            return storageClient.getCurrentQuotas(userEmail);
        }, this::toResource);
    }

    @GetMapping(value = "/email/{user_email}/admin", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to check if user has administrator role", role = DefaultRole.PUBLIC)
    @Operation(summary = "Check the role of user",
               description = "Check if the user with the given email has administrator role")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "True if the user has an administrator role, false otherwise") })
    public ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") String userEmail) {
        return projectUsersClient.isAdmin(userEmail);
    }

    /**
     * Update the {@link ProjectUserReadDto} of id <code>pUserId</code>.
     *
     * @param userId             The {@link ProjectUserReadDto} <code>id</code>
     * @param updatedProjectUser The new {@link ProjectUserReadDto}
     * @return void
     */
    @PutMapping(value = USER_ID_RELATIVE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to update the project user", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Update the project user", description = "Update the project user with the given identifier")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Return the updated project user") })
    public ResponseEntity<EntityModel<ProjectUserReadDto>> updateProjectUser(@PathVariable("user_id") Long userId,
                                                                             @RequestBody
                                                                             ProjectUserUpdateDto updatedProjectUser)
        throws ModuleException {
        String userEmail = updatedProjectUser.getEmail();
        Tuple2<ProjectUser, DownloadQuotaLimitsDto> t = makeProjectUserAndQuotaLimitsDto(updatedProjectUser);
        return combineQuotaThenProjectUserCalls(userEmail, () -> {
            FeignSecurityManager.asUser(authenticationResolver.getUser(), RoleAuthority.getSysRole(appName));
            return storageClient.upsertQuotaLimits(userEmail, t._2);
        }, () -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.updateProjectUser(userId, t._1);
        }, this::toResource);
    }

    /**
     * Update the {@link ProjectUserReadDto} of current projet user authenticated.
     *
     * @param updatedProjectUser The new {@link ProjectUserReadDto}
     * @return void
     */
    @PutMapping(value = "/myuser", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to update the currently authenticated project user",
                    role = DefaultRole.REGISTERED_USER)
    @Operation(summary = "Update the current project user",
               description = "Update the currently authenticated project user")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Returns the currently authenticated project user") })
    public ResponseEntity<EntityModel<ProjectUserReadDto>> updateCurrentProjectUser(
        @RequestBody ProjectUserUpdateDto updatedProjectUser) throws ModuleException {
        String userEmail = authenticationResolver.getUser();
        Tuple2<ProjectUser, DownloadQuotaLimitsDto> t = makeProjectUserAndQuotaLimitsDto(updatedProjectUser);
        return combineQuotaThenProjectUserCalls(userEmail, () -> {
            FeignSecurityManager.asUser(authenticationResolver.getUser(), RoleAuthority.getSysRole(appName));
            return storageClient.upsertQuotaLimits(userEmail, t._2);
        }, () -> projectUsersClient.updateCurrentProjectUser(t._1), this::toResourceRegisteredUser);
    }

    /**
     * Create a new user by bypassing registration process (accounts and projectUser validation)
     *
     * @param projectUserCreateDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to create a project user by bypassing the registration process "
                                  + "(Administrator feature)", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Create a project user",
               description = "Create a project user by bypassing the registration process (Administrator feature)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns the created project user") })
    public ResponseEntity<EntityModel<ProjectUserReadDto>> createUser(
        @Valid @RequestBody ProjectUserCreateDto projectUserCreateDto) throws ModuleException {
        String email = projectUserCreateDto.getEmail();
        DownloadQuotaLimitsDto limits = new DownloadQuotaLimitsDto(email,
                                                                   projectUserCreateDto.getMaxQuota(),
                                                                   projectUserCreateDto.getRateLimit());
        AccessRequestDto accessRequestDto = new AccessRequestDto().setEmail(email)
                                                                  .setFirstName(projectUserCreateDto.getFirstName())
                                                                  .setLastName(projectUserCreateDto.getLastName())
                                                                  .setRoleName(projectUserCreateDto.getRoleName())
                                                                  .setMetadata(projectUserCreateDto.getMetadata())
                                                                  .setPassword(projectUserCreateDto.getPassword())
                                                                  .setOriginUrl(projectUserCreateDto.getOriginUrl())
                                                                  .setRequestLink(projectUserCreateDto.getRequestLink())
                                                                  .setAccessGroups(projectUserCreateDto.getAccessGroups())
                                                                  .setMaxQuota(projectUserCreateDto.getMaxQuota());
        return combineQuotaThenProjectUserCalls(email, () -> {
            FeignSecurityManager.asUser(authenticationResolver.getUser(), RoleAuthority.getSysRole(appName));
            return storageClient.upsertQuotaLimits(email, limits);
        }, () -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.createUser(accessRequestDto);
        }, this::toResourceRegisteredUser);
    }

    /**
     * Delete the {@link ProjectUserReadDto} of passed <code>id</code>.
     *
     * @param userId The {@link ProjectUserReadDto}'s <code>id</code>
     * @return void
     */
    @DeleteMapping(USER_ID_RELATIVE_PATH)
    @ResourceAccess(description = "Endpoint to remove the project user", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Delete the project user", description = "Delete the project user with the given identifier")
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId) throws ModuleException {
        String errorMessage = String.format("User %s could no be retrieved from admin service", userId);
        try {
            ResponseEntity<EntityModel<ProjectUser>> response = projectUsersClient.retrieveProjectUser(userId);
            EntityModel<ProjectUser> responseBody = ResponseEntityUtils.extractBodyOrThrow(response, errorMessage);
            if (response.getStatusCode() == HttpStatus.OK) {
                ProjectUser projectUser = responseBody.getContent();
                if (projectUser != null) {
                    searchHistoryService.deleteAccountSearchHistory(projectUser.getEmail());
                    return projectUsersClient.removeProjectUser(userId);
                } else {
                    throw new ModuleException(errorMessage);
                }
            } else {
                throw new ModuleException(errorMessage);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(errorMessage);
        }
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUserReadDto} for the role of passed
     * <code>id</code> by crawling through parents' hierarachy.
     *
     * @param roleId    The role's <code>id</code>
     * @param pageable  paging info
     * @param assembler assembler info
     * @return The {@link List} of {@link ProjectUserReadDto} wrapped in an {@link ResponseEntity}
     */
    @GetMapping(value = ROLES_ROLE_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieve the list of project users with the specified role identifier "
                                  + "or a parent role", role = DefaultRole.ADMIN)
    @Operation(summary = "Get the list of project users",
               description = "Retrieve a page of project users with the specified role identifier or a parent role")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Returns the list of project users with "
                                                       + "the specified role or a parent role") })
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveRoleProjectUserList(
        @PathVariable("role_id") Long roleId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {
        return completeUserPagedResponseWithQuotas(() -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.retrieveRoleProjectUserList(roleId, pageable);
        }, pageable, assembler);
    }

    /**
     * Define the endpoint for retrieving the {@link List} of {@link ProjectUserReadDto} for the role of passed
     * <code>name</code> by crawling through parents' hierarachy.
     *
     * @param role      The role's <code>name</code>
     * @param pageable  paging info
     * @param assembler assembler info
     * @return The {@link List} of {@link ProjectUserReadDto} wrapped in an {@link ResponseEntity}
     */
    @GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieve the list of project users with the specified role name "
                                  + "or a parent role", role = DefaultRole.ADMIN)
    @Operation(summary = "Get the list of project users",
               description = "Retrieve a page containing the list of project users with the specified role name "
                             + "or a parent role")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns the list of project users") })
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveRoleProjectUsersList(
        @RequestParam("role_name") String role,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {
        return completeUserPagedResponseWithQuotas(() -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.retrieveRoleProjectUsersList(role, pageable);
        }, pageable, assembler);
    }

    @GetMapping(value = "/email/{email}/verification/resend")
    @ResourceAccess(description = "Send a new verification email for a user creation", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Valid a user creation", description = "Send a new verification email for a user creation")
    public ResponseEntity<Void> sendVerificationEmail(@PathVariable("email") String email) {
        projectUsersClient.sendVerificationEmail(email);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> completeUserPagedResponseWithQuotas(Supplier<ResponseEntity<PagedModel<EntityModel<ProjectUser>>>> usersRequest,
                                                                                                            Pageable pageable,
                                                                                                            PagedResourcesAssembler<ProjectUserReadDto> pagedResourcesAssembler)
        throws ModuleException {
        AtomicReference<PagedModel.PageMetadata> meta = new AtomicReference<>();
        AtomicReference<io.vavr.collection.List<ProjectUser>> users = new AtomicReference<>(io.vavr.collection.List.empty());
        return toResponse(Try.ofSupplier(usersRequest)
                             .andFinally(FeignSecurityManager::reset)
                             .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                             .peek(pagedModel -> meta.set(pagedModel.getMetadata())) // need a piece of state (pagination metadata) for later if success
                             .map(PagedModel::getContent)
                             .map(entityModels -> entityModels.stream()
                                                              .map(EntityModel::getContent)
                                                              // fill the list with users while mapping, we'll need them later
                                                              .peek(projectUser -> users.updateAndGet(list -> list.append(
                                                                  projectUser)))
                                                              .map(ProjectUser::getEmail)
                                                              .toArray(String[]::new))
                             .flatMap(emails -> Try.run(() -> FeignSecurityManager.asUser(authenticationResolver.getUser(),
                                                                                          RoleAuthority.getSysRole(
                                                                                              appName)))
                                                   .map(unused -> storageClient.getCurrentQuotasList(emails))
                                                   .andFinally(FeignSecurityManager::reset)
                                                   .map(ResponseEntity::getBody)
                                                   // special value for frontend if any error on storage or storage not deploy
                                                   .onFailure(throwable -> LOGGER.debug(
                                                       "Failed to query rs-storage for quotas.",
                                                       throwable))
                                                   .orElse(() -> Try.success(Arrays.stream(emails)
                                                                                   .map(UserCurrentQuotasDto::new)
                                                                                   .collect(toList())))
                                                   .toValidation(ComposableClientException::make))
                             .map(quotas -> users.get()
                                                 .zip(quotas)
                                                 .map(ul -> new ProjectUserReadDto(ul._1, ul._2))
                                                 .toJavaList())
                             .map(list -> new PageImpl<>(list, pageable, meta.get().getTotalElements()))
                             .map(page -> toPagedResources(page, pagedResourcesAssembler))
                             .map(paged -> new ResponseEntity<>(paged, HttpStatus.OK))
                             .mapError(ModuleException::new));
    }

    private Tuple2<ProjectUser, DownloadQuotaLimitsDto> makeProjectUserAndQuotaLimitsDto(ProjectUserUpdateDto updatedProjectUser) {

        String userEmail = updatedProjectUser.getEmail();

        ProjectUser user = new ProjectUser().setId(updatedProjectUser.getId())
                                            .setEmail(userEmail)
                                            .setFirstName(updatedProjectUser.getFirstName())
                                            .setLastName(updatedProjectUser.getLastName())
                                            .setPermissions(updatedProjectUser.getPermissions())
                                            .setMetadata(new HashSet<>(updatedProjectUser.getMetadata()))
                                            .setRole(updatedProjectUser.getRole())
                                            .setAccessGroups(updatedProjectUser.getAccessGroups())
                                            .setMaxQuota(updatedProjectUser.getMaxQuota());

        DownloadQuotaLimitsDto limits = new DownloadQuotaLimitsDto(userEmail,
                                                                   updatedProjectUser.getMaxQuota(),
                                                                   updatedProjectUser.getRateLimit());

        return Tuple.of(user, limits);
    }

    private ResponseEntity<EntityModel<ProjectUserReadDto>> combineProjectUserThenQuotaCalls(Supplier<ResponseEntity<EntityModel<ProjectUser>>> projectUsersCall,
                                                                                             Supplier<ResponseEntity<UserCurrentQuotasDto>> quotaCall,
                                                                                             Function<ProjectUserReadDto, EntityModel<ProjectUserReadDto>> resourceMapper)
        throws ModuleException {
        return toResponse(Try.ofSupplier(projectUsersCall)
                             .andFinally(FeignSecurityManager::reset)
                             .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                             .map(EntityModel::getContent)
                             .combine(Try.ofSupplier(quotaCall)
                                         .andFinally(FeignSecurityManager::reset)
                                         .transform(ignoreStorageQuotaErrors))
                             .ap(ProjectUserReadDto::new)
                             .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                             .map(resourceMapper)
                             .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK)));
    }

    private ResponseEntity<EntityModel<ProjectUserReadDto>> combineQuotaThenProjectUserCalls(String userEmail,
                                                                                             Supplier<ResponseEntity<DownloadQuotaLimitsDto>> quotaLimitsCall,
                                                                                             Supplier<ResponseEntity<EntityModel<ProjectUser>>> projectUsersCall,
                                                                                             Function<ProjectUserReadDto, EntityModel<ProjectUserReadDto>> resourceMapper)
        throws ModuleException {

        // Try to update storage quota
        Validation<ComposableClientException, UserCurrentQuotasDto> updateStorage = Try.ofSupplier(quotaLimitsCall)
                                                                     .andFinally(FeignSecurityManager::reset)
                                                                     .map(unit -> {
                                                                         FeignSecurityManager.asUser(
                                                                             authenticationResolver.getUser(),
                                                                             RoleAuthority.getSysRole(appName));
                                                                         return storageClient.getCurrentQuotas(userEmail);
                                                                     })
                                                                     .andFinally(FeignSecurityManager::reset)
                                                                                       .transform(handleClientFailure(STORAGE_CLIENT));

        if (updateStorage.isValid()) {
            Validation<ComposableClientException, ProjectUser> updateAdmin = Try.ofSupplier(projectUsersCall)
                                                                                .andFinally(FeignSecurityManager::reset)
                                                                                .transform(handleClientFailure(
                                                                                    ACCESSRIGHTS_CLIENT))
                                                                                .map(EntityModel::getContent);
            return toResponse(updateStorage.combine(updateAdmin)
                                  .ap(ProjectUserReadDto::new)
                                  .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                                  .map(resourceMapper)
                                  .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK)));
        } else {
            LOGGER.error("Unable to update rs-storage quota.", updateStorage.getError().getCause());
            throw new ModuleException("Unable to update rs-storage quota.");
        }
    }

    private <V> V toResponse(Validation<ModuleException, V> v) throws ModuleException {
        if (v.isValid()) {
            return v.get();
        } else {
            throw v.getError();
        }
    }

    @Override
    public EntityModel<ProjectUserReadDto> toResource(final ProjectUserReadDto userResourceDto,
                                                      final Object... extras) {
        EntityModel<ProjectUserReadDto> resource = resourceService.toResource(userResourceDto);
        if (userResourceDto != null && userResourceDto.getId() != null) {
            addHateoasLinks(userResourceDto, resource);
        }
        return resource;
    }

    @Override
    public PagedModel<EntityModel<ProjectUserReadDto>> toPagedResources(final Page<ProjectUserReadDto> elements,
                                                                        final PagedResourcesAssembler<ProjectUserReadDto> assembler,
                                                                        final Object... extras) {
        Preconditions.checkNotNull(elements);
        final PagedModel<EntityModel<ProjectUserReadDto>> pageResources = assembler.toModel(elements);
        Set<Role> currentUserAscendantRoles = getCurrentLoggedUserAscendantRoles();
        pageResources.forEach(resource -> resource.add(toResourceWithAccessRights(resource.getContent(),
                                                                                  currentUserAscendantRoles,
                                                                                  extras).getLinks()));
        return pageResources;
    }

    /**
     * Special HATEOAS method to add resource access restrictions to the current logged user.
     * The latter should have at least a superior role to access user resources.
     */
    public EntityModel<ProjectUserReadDto> toResourceWithAccessRights(final ProjectUserReadDto userResourceDto,
                                                                      Set<Role> currentUserAscendantRoles,
                                                                      final Object... extras) {
        EntityModel<ProjectUserReadDto> resource = resourceService.toResource(userResourceDto);
        if (userResourceDto != null && userResourceDto.getId() != null && hasAccessToRole(userResourceDto.getRole(),
                                                                                          currentUserAscendantRoles)) {
            addHateoasLinks(userResourceDto, resource);
        }
        return resource;
    }

    private void addHateoasLinks(ProjectUserReadDto userResourceDto, EntityModel<ProjectUserReadDto> resource) {
        MethodParam<Long> idParam = MethodParamFactory.build(Long.class, userResourceDto.getId());
        Class<? extends ProjectUsersController> clazz = this.getClass();

        resourceService.addLink(resource, clazz, "retrieveProjectUser", LinkRels.SELF, idParam);
        resourceService.addLink(resource,
                                clazz,
                                "updateProjectUser",
                                LinkRels.UPDATE,
                                idParam,
                                MethodParamFactory.build(ProjectUserUpdateDto.class));
        resourceService.addLink(resource, clazz, "removeProjectUser", LinkRels.DELETE, idParam);
        resourceService.addLink(resource,
                                clazz,
                                "retrieveProjectUserList",
                                LinkRels.LIST,
                                MethodParamFactory.build(SearchProjectUserParameters.class),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));

        if (UserStatus.WAITING_ACCESS.equals(userResourceDto.getStatus())) {
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
        if (UserStatus.ACCESS_GRANTED.equals(userResourceDto.getStatus())) {
            resourceService.addLink(resource,
                                    RegistrationController.class,
                                    "inactiveAccess",
                                    LinkRelation.of("inactive"),
                                    idParam);
        }
        if (UserStatus.ACCESS_DENIED.equals(userResourceDto.getStatus())) {
            resourceService.addLink(resource,
                                    RegistrationController.class,
                                    "acceptAccessRequest",
                                    LinkRelation.of("accept"),
                                    idParam);
        }
        if (UserStatus.ACCESS_INACTIVE.equals(userResourceDto.getStatus())) {
            resourceService.addLink(resource,
                                    RegistrationController.class,
                                    "activeAccess",
                                    LinkRelation.of("active"),
                                    idParam);
        }
        if (UserStatus.WAITING_EMAIL_VERIFICATION.equals(userResourceDto.getStatus())) {
            resourceService.addLink(resource,
                                    clazz,
                                    "sendVerificationEmail",
                                    LinkRelation.of("sendVerificationEmail"),
                                    MethodParamFactory.build(String.class, userResourceDto.getEmail()));
        }
    }

    /**
     * Special HATEOAS resource maker for registered users asking for their own users. The toResource method is for project admins.
     *
     * @param projectUser {@link ProjectUserReadDto} to transform to HATEOAS resources.
     * @return HATEOAS resources for {@link ProjectUserReadDto}
     */
    public EntityModel<ProjectUserReadDto> toResourceRegisteredUser(ProjectUserReadDto projectUser) {

        EntityModel<ProjectUserReadDto> resource = resourceService.toResource(projectUser);

        if ((projectUser != null) && (projectUser.getId() != null)) {
            resourceService.addLink(resource, this.getClass(), "retrieveCurrentProjectUser", LinkRels.SELF);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "updateCurrentProjectUser",
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(ProjectUserUpdateDto.class));
        }
        return resource;
    }

    /**
     * Check if the user modifications are accessible by the current logged user.
     * The latter must have at least the same role.
     *
     * @param targetRole                user role to be checked
     * @param currentUserAscendantRoles all roles accessible for the current logged user
     * @return true if the logged user has access to the target user
     */
    private boolean hasAccessToRole(Role targetRole, Set<Role> currentUserAscendantRoles) {
        boolean hasAccess = false;
        if (!CollectionUtils.isEmpty(currentUserAscendantRoles)) {
            // check if logged user has a role superior or equal to target user
            // OR if logged user is INSTANCE_ADMIN (because this role does not have any ascendant)
            hasAccess = currentUserAscendantRoles.stream()
                                                 .anyMatch(role -> role.equals(targetRole)
                                                                   || DefaultRole.INSTANCE_ADMIN.toString()
                                                                                                .equals(role.getName()));
        }
        return hasAccess;
    }

    /**
     * Retrieve the current logged user's ascendant roles
     */
    private Set<Role> getCurrentLoggedUserAscendantRoles() {
        Set<Role> userAscendantRoles = new HashSet<>();
        String currentUserEmail = authenticationResolver.getUser();
        if (!StringUtils.isBlank(currentUserEmail)) {

            try {
                FeignSecurityManager.asUser(currentUserEmail, RoleAuthority.getSysRole(appName));
                ResponseEntity<EntityModel<ProjectUserReadDto>> response = this.retrieveProjectUserByEmail(
                    currentUserEmail);
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    ProjectUserReadDto currentUserDto = ResponseEntityUtils.extractContentOrThrow(response,
                                                                                                  "An error occurred while trying to "
                                                                                                  + "retrieve the specific user");
                    userAscendantRoles = getAscendantRoles(currentUserDto);
                }
            } catch (ModuleException e) {
                LOGGER.warn("User \"{}\" not found. Unable to check his access to actions.", currentUserEmail, e);
            } finally {
                FeignSecurityManager.reset();
            }

        } else {
            LOGGER.warn("Not able to acquire current user \"{}\" from authentication resolver. No access to "
                        + " user resources granted !", currentUserEmail);
        }
        return userAscendantRoles;
    }

    /**
     * Retrieve all ascendant roles of a user
     */
    private Set<Role> getAscendantRoles(ProjectUserReadDto userDto) {
        Set<Role> ascendantRoles = null;
        ResponseEntity<Set<Role>> response = rolesClient.retrieveRoleAscendants(userDto.getRole().getName());
        if (response != null && response.getStatusCode().is2xxSuccessful()) {
            ascendantRoles = ResponseEntityUtils.extractBodyOrNull(response);
        }
        if (ascendantRoles == null) {
            LOGGER.warn("Ascendant roles for role \"{}\" not found. Unable to check his access to actions.",
                        userDto.getEmail());
        }
        return ascendantRoles;
    }

}

