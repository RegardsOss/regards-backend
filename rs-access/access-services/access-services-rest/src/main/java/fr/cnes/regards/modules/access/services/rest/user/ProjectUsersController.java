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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserCreateDto;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserReadDto;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserUpdateDto;
import fr.cnes.regards.modules.access.services.rest.user.utils.ComposableClientException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.HashSet;
import java.util.List;
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
@RestController
@RequestMapping(ProjectUsersController.TYPE_MAPPING)
public class ProjectUsersController implements IResourceController<ProjectUserReadDto> {

    public static final String TYPE_MAPPING = "/users";

    public static final String USER_ID_RELATIVE_PATH = "/{user_id}";

    public static final String ROLES_ROLE_ID = "/roles/{role_id}";

    public static final String PENDINGACCESSES = "/pendingaccesses";

    public static final String ACCESSRIGHTS_CLIENT = "accessrights-client";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUsersController.class);

    private final IProjectUsersClient projectUsersClient;

    private final IStorageRestClient storageClient;

    private final IResourceService resourceService;

    private final IAuthenticationResolver authenticationResolver;

    private final Function<Try<ResponseEntity<UserCurrentQuotas>>, Validation<ComposableClientException, UserCurrentQuotas>> ignoreStorageQuotaErrors = t -> t.map(
                                                                                                                                                                  ResponseEntity::getBody)
                                                                                                                                                              // special value for frontend if any error on storage or storage not deploy
                                                                                                                                                              .onFailure(
                                                                                                                                                                  e -> LOGGER.debug(
                                                                                                                                                                      "Failed to query rs-storage for quotas.",
                                                                                                                                                                      e))
                                                                                                                                                              .orElse(
                                                                                                                                                                  () -> Try.success(
                                                                                                                                                                      new UserCurrentQuotas()))
                                                                                                                                                              .toValidation(
                                                                                                                                                                  ComposableClientException::make);

    @Value("${spring.application.name}")
    private String appName;

    public ProjectUsersController(IProjectUsersClient projectUsersClient,
                                  IStorageRestClient storageClient,
                                  IResourceService resourceService,
                                  IAuthenticationResolver authenticationResolver) {
        this.projectUsersClient = projectUsersClient;
        this.storageClient = storageClient;
        this.resourceService = resourceService;
        this.authenticationResolver = authenticationResolver;
    }

    /**
     * Retrieve the {@link List} of all {@link ProjectUserReadDto}s.
     *
     * @param pageable   paging info
     * @param assembler  assembler info
     * @param parameters search parameters
     * @return a {@link List} of {@link ProjectUserReadDto}
     */
    @GetMapping
    @ResourceAccess(description = "retrieve the list of users of the project", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveProjectUserList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserReadDto> assembler,
        ProjectUserSearchParameters parameters) throws ModuleException {
        return completeUserPagedResponseWithQuotas(() -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.retrieveProjectUserList(parameters, pageable);
        }, pageable, assembler);
    }

    /**
     * Retrieve all users with a pending access request.
     *
     * @param pageable  paging info
     * @param assembler assembler info
     * @return The {@link List} of all {@link ProjectUserReadDto}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @GetMapping(PENDINGACCESSES)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveAccessRequestList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {
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
    @GetMapping(USER_ID_RELATIVE_PATH)
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
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
    @GetMapping("/myuser")
    @ResourceAccess(description = "retrieve the current authenticated project user and only display  metadata",
        role = DefaultRole.REGISTERED_USER)
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
    @GetMapping("/email/{user_email}")
    @ResourceAccess(description = "retrieve the project user and only display  metadata", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUserReadDto>> retrieveProjectUserByEmail(
        @PathVariable("user_email") String userEmail) throws ModuleException {
        return combineProjectUserThenQuotaCalls(() -> projectUsersClient.retrieveProjectUserByEmail(userEmail), () -> {
            FeignSecurityManager.asUser(authenticationResolver.getUser(), RoleAuthority.getSysRole(appName));
            return storageClient.getCurrentQuotas(userEmail);
        }, this::toResource);
    }

    @GetMapping("/email/{user_email}/admin")
    @ResourceAccess(description = "tell if user has role admin", role = DefaultRole.PUBLIC)
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
    @PutMapping(USER_ID_RELATIVE_PATH)
    @ResourceAccess(description = "update the project user", role = DefaultRole.EXPLOIT)
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
    @PutMapping("/myuser")
    @ResourceAccess(description = "Update the current authenticated project user", role = DefaultRole.REGISTERED_USER)
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
    @PostMapping
    @ResourceAccess(description = "Create a projectUser by bypassing registration process (Administrator feature)",
        role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<ProjectUserReadDto>> createUser(@Valid @RequestBody
                                                                      ProjectUserCreateDto projectUserCreateDto)
        throws ModuleException {
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
    @ResourceAccess(description = "remove the project user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId) {
        return projectUsersClient.removeProjectUser(userId);
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
    @GetMapping(ROLES_ROLE_ID)
    @ResourceAccess(
        description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_id",
        role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveRoleProjectUserList(
        @PathVariable("role_id") Long roleId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {
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
    @GetMapping("/roles")
    @ResourceAccess(
        description = "Retrieve the list of project users (crawls through parents' hierarchy) of the role with role_name",
        role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectUserReadDto>>> retrieveRoleProjectUsersList(
        @RequestParam("role_name") String role,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectUserReadDto> assembler) throws ModuleException {
        return completeUserPagedResponseWithQuotas(() -> {
            FeignSecurityManager.asSystem();
            return projectUsersClient.retrieveRoleProjectUsersList(role, pageable);
        }, pageable, assembler);
    }

    @GetMapping("/email/{email}/verification/resend")
    @ResourceAccess(description = "Send a new verification email for a user creation", role = DefaultRole.EXPLOIT)
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
                                                                                   .map(UserCurrentQuotas::new)
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
                                                                                             Supplier<ResponseEntity<UserCurrentQuotas>> quotaCall,
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
        return toResponse(Try.ofSupplier(quotaLimitsCall)
                             .andFinally(FeignSecurityManager::reset)
                             .map(unit -> {
                                 FeignSecurityManager.asUser(authenticationResolver.getUser(),
                                                             RoleAuthority.getSysRole(appName));
                                 return storageClient.getCurrentQuotas(userEmail);
                             })
                             .andFinally(FeignSecurityManager::reset)
                             .transform(ignoreStorageQuotaErrors)
                             .combine(Try.ofSupplier(projectUsersCall)
                                         .andFinally(FeignSecurityManager::reset)
                                         .transform(handleClientFailure(ACCESSRIGHTS_CLIENT))
                                         .map(EntityModel::getContent))
                             .ap(ProjectUserReadDto::new)
                             .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                             .map(resourceMapper)
                             .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK)));
    }

    private <V> V toResponse(Validation<ModuleException, V> v) throws ModuleException {
        if (v.isValid()) {
            return v.get();
        } else {
            throw v.getError();
        }
    }

    @Override
    public EntityModel<ProjectUserReadDto> toResource(final ProjectUserReadDto element, final Object... extras) {

        EntityModel<ProjectUserReadDto> resource = resourceService.toResource(element);

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
                                    MethodParamFactory.build(ProjectUserUpdateDto.class));
            resourceService.addLink(resource, clazz, "removeProjectUser", LinkRels.DELETE, idParam);
            resourceService.addLink(resource,
                                    clazz,
                                    "retrieveProjectUserList",
                                    LinkRels.LIST,
                                    MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class),
                                    MethodParamFactory.build(ProjectUserSearchParameters.class));

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
            if (UserStatus.ACCESS_GRANTED.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "inactiveAccess",
                                        LinkRelation.of("inactive"),
                                        idParam);
            }
            if (UserStatus.ACCESS_DENIED.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        RegistrationController.class,
                                        "acceptAccessRequest",
                                        LinkRelation.of("accept"),
                                        idParam);
            }
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
                                        MethodParamFactory.build(String.class, element.getEmail()));
            }
        }
        return resource;
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
            resource = resourceService.toResource(projectUser);
            resourceService.addLink(resource, this.getClass(), "retrieveCurrentProjectUser", LinkRels.SELF);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "updateCurrentProjectUser",
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(ProjectUserUpdateDto.class));
        }
        return resource;
    }
}
