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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.ProjectUserSpecificationsBuilder;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.*;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserEvent;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.RegardsStreamUtils;
import fr.cnes.regards.modules.accessrights.service.config.AccessRightsTemplateConfiguration;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailService;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailWrapper;
import fr.cnes.regards.modules.accessrights.service.utils.AccountUtilsService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link IProjectUserService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class ProjectUserService implements IProjectUserService {

    public static final String PUBLIC_USER_EMAIL = "public@regards.com";

    /**
     * A filter on metadata to keep visible ones only
     */
    public static final Predicate<? super MetaData> KEEP_VISIBLE_META_DATA = metaData -> !UserVisibility.HIDDEN.equals(
        metaData.getVisibility());

    private static final Logger LOG = LoggerFactory.getLogger(ProjectUserService.class);

    private final IProjectUserRepository projectUserRepository;

    private final IRoleService roleService;

    private final IAuthenticationResolver authenticationResolver;

    private final ApplicationEventPublisher eventPublisher;

    private final AccessSettingsService accessSettingsService;

    private final AccountUtilsService accountUtilsService;

    private final AccessRightsEmailService accessRightsEmailService;

    private final ProjectUserGroupService projectUserGroupService;

    private final QuotaHelperService quotaHelperService;

    private final IPublisher publisher;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    /**
     * Configured instance administrator user email/login
     */
    @Value("${regards.accounts.root.user.login}")
    private String instanceAdminUserEmail;

    public ProjectUserService(IAuthenticationResolver authenticationResolver,
                              IProjectUserRepository projectUserRepository,
                              IRoleService roleService,
                              ApplicationEventPublisher eventPublisher,
                              AccessSettingsService accessSettingsService,
                              AccountUtilsService accountUtilsService,
                              AccessRightsEmailService accessRightsEmailService,
                              ProjectUserGroupService projectUserGroupService,
                              QuotaHelperService quotaHelperService,
                              IEmailVerificationTokenService emailVerificationTokenService,
                              IPublisher publisher) {
        this.authenticationResolver = authenticationResolver;
        this.projectUserRepository = projectUserRepository;
        this.roleService = roleService;
        this.eventPublisher = eventPublisher;
        this.accessSettingsService = accessSettingsService;
        this.accountUtilsService = accountUtilsService;
        this.accessRightsEmailService = accessRightsEmailService;
        this.projectUserGroupService = projectUserGroupService;
        this.quotaHelperService = quotaHelperService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.publisher = publisher;
    }

    @Override
    public Page<ProjectUser> retrieveUsers(SearchProjectUserParameters filters, Pageable pageable) {
        return projectUserRepository.findAll(new ProjectUserSpecificationsBuilder().withParameters(filters).build(),
                                             pageable);
    }

    @Override
    public ProjectUser retrieveUser(Long userId) throws EntityNotFoundException {
        Optional<ProjectUser> userOpt = projectUserRepository.findById(userId);
        // Check found
        if (!userOpt.isPresent()) {
            throw new EntityNotFoundException(userId.toString(), ProjectUser.class);
        }
        // Filter out hidden meta data
        ProjectUser user = userOpt.get();
        user.setMetadata(user.getMetadata().stream().filter(KEEP_VISIBLE_META_DATA).collect(Collectors.toSet()));
        return user;
    }

    @Override
    public ProjectUser retrieveOneByEmail(String userEmail) throws EntityNotFoundException {
        return retrieveOneOptionalByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException(userEmail,
                                                                                                   ProjectUser.class));
    }

    @Override
    public Optional<ProjectUser> retrieveOneOptionalByEmail(String userEmail) {
        Optional<ProjectUser> user;
        if (instanceAdminUserEmail.equals(userEmail)) {
            user = Optional.of(new ProjectUser().setEmail(userEmail)
                                                .setRole(new Role(DefaultRole.INSTANCE_ADMIN.toString(), null)));
        } else {
            user = projectUserRepository.findOneByEmail(userEmail);
            // Filter out hidden meta data
            if (user.isPresent()) {
                Set<MetaData> visibleMetadata = user.get()
                                                    .getMetadata()
                                                    .stream()
                                                    .filter(KEEP_VISIBLE_META_DATA)
                                                    .collect(Collectors.toSet());
                user.get().setMetadata(visibleMetadata);
            }
        }
        return user;
    }

    @Override
    public ProjectUser retrieveCurrentUser() throws EntityNotFoundException {
        String email = authenticationResolver.getUser();
        return projectUserRepository.findOneByEmail(email)
                                    .orElseThrow(() -> new EntityNotFoundException("Current user", ProjectUser.class));
    }

    @Override
    public Page<ProjectUser> retrieveAccessRequestList(Pageable pageable) {
        return projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS, pageable);
    }

    @Override
    public ProjectUser updateUser(Long userId, ProjectUser updatedProjectUser) throws EntityException {

        ProjectUser projectUser = retrieveUser(userId);
        if (!updatedProjectUser.getId().equals(userId)) {
            throw new EntityInconsistentIdentifierException(userId, updatedProjectUser.getId(), ProjectUser.class);
        }

        // Check that no public group is removed or added
        if (CollectionUtils.isEmpty(updatedProjectUser.getAccessGroups())) {
            projectUserGroupService.validateAccessGroups(projectUser.getAccessGroups(), true);
        } else {
            projectUserGroupService.validateAccessGroups(Sets.symmetricDifference(projectUser.getAccessGroups(),
                                                                                  updatedProjectUser.getAccessGroups()),
                                                         true);
        }

        publisher.publish(new ProjectUserEvent(updatedProjectUser.getEmail(), ProjectUserAction.UPDATE));
        return save(updatedProjectUser);
    }

    @Override
    public ProjectUser updateUserInfos(Long userId, ProjectUser updatedProjectUser) throws EntityException {

        ProjectUser projectUser = retrieveUser(userId);
        if (!updatedProjectUser.getId().equals(userId)) {
            throw new EntityInconsistentIdentifierException(userId, updatedProjectUser.getId(), ProjectUser.class);
        }

        Role newRole = updatedProjectUser.getRole();
        if (newRole != null && newRole.getId() == null && StringUtils.isNotBlank(newRole.getName())) {
            newRole = roleService.retrieveRole(updatedProjectUser.getRole().getName());
        }
        if (newRole != null) {
            projectUser.setRole(newRole);
        }
        projectUser.setMetadata(updatedProjectUser.getMetadata());
        projectUser.setPermissions(updatedProjectUser.getPermissions());

        Long newMaxQuota = updatedProjectUser.getMaxQuota();
        if (newMaxQuota != null && newMaxQuota > -2) {
            projectUser.setMaxQuota(newMaxQuota);
        }
        if (updatedProjectUser.getLastConnection() != null) {
            projectUser.setLastConnection(updatedProjectUser.getLastConnection());
        }

        // Check that no public group is removed or added
        Set<String> accessGroups = updatedProjectUser.getAccessGroups();
        if (!CollectionUtils.isEmpty(accessGroups)) {
            projectUserGroupService.validateAccessGroups(Sets.symmetricDifference(projectUser.getAccessGroups(),
                                                                                  accessGroups), true);
            projectUser.setAccessGroups(accessGroups);
        }

        publisher.publish(new ProjectUserEvent(updatedProjectUser.getEmail(), ProjectUserAction.UPDATE));
        return save(projectUser);
    }

    @Override
    public void updateUserAccessRights(String login, List<ResourcesAccess> updatedUserAccessRights)
        throws EntityNotFoundException {
        ProjectUser user = projectUserRepository.findOneByEmail(login)
                                                .orElseThrow(() -> new EntityNotFoundException(login,
                                                                                               ProjectUser.class));
        try (Stream<ResourcesAccess> previous = user.getPermissions().stream();
            Stream<ResourcesAccess> updated = updatedUserAccessRights.stream();
            Stream<ResourcesAccess> merged = Stream.concat(updated, previous)) {
            user.setPermissions(merged.filter(RegardsStreamUtils.distinctByKey(ResourcesAccess::getId))
                                      .collect(Collectors.toList()));
        }
        save(user);
    }

    @Override
    public void removeUserAccessRights(String login) throws EntityNotFoundException {
        ProjectUser user = projectUserRepository.findOneByEmail(login)
                                                .orElseThrow(() -> new EntityNotFoundException(login,
                                                                                               ProjectUser.class));
        user.setPermissions(new ArrayList<>());
        save(user);
    }

    @Override
    public List<MetaData> retrieveUserMetaData(Long userId) throws EntityNotFoundException {
        ProjectUser user = retrieveUser(userId);
        return new ArrayList<>(user.getMetadata());
    }

    @Override
    public List<MetaData> updateUserMetaData(Long userId, List<MetaData> updatedUserMetaData)
        throws EntityNotFoundException {
        ProjectUser user = retrieveUser(userId);
        user.setMetadata(new HashSet<>(updatedUserMetaData));
        ProjectUser savedUser = save(user);
        return new ArrayList<>(savedUser.getMetadata());
    }

    @Override
    public void removeUserMetaData(Long userId) throws EntityNotFoundException {
        ProjectUser user = retrieveUser(userId);
        user.setMetadata(new HashSet<>());
        save(user);
    }

    @Override
    public List<ResourcesAccess> retrieveProjectUserAccessRights(String email, String borrowedRoleName)
        throws EntityException {

        ProjectUser projectUser = retrieveOneByEmail(email);
        Role returnedRole = projectUser.getRole();

        if (borrowedRoleName != null) {
            Role borrowedRole = roleService.retrieveRole(borrowedRoleName);
            if (roleService.isHierarchicallyInferior(borrowedRole, returnedRole)) {
                returnedRole = borrowedRole;
            } else {
                throw new EntityOperationForbiddenException(borrowedRoleName,
                                                            Role.class,
                                                            "Borrowed role must be hierachically inferior to the project user's role");
            }
        }

        // Merge permissions from the project user and from the role
        List<ResourcesAccess> fromUser = projectUser.getPermissions();
        List<ResourcesAccess> merged = new ArrayList<>(fromUser);
        try {
            Set<ResourcesAccess> fromRole = roleService.retrieveRoleResourcesAccesses(returnedRole.getId());
            merged.addAll(fromRole);
        } catch (EntityNotFoundException e) {
            LOG.debug("Could not retrieve permissions from role", e);
        }
        return merged;
    }

    @Override
    public ProjectUser createProjectUser(AccessRequestDto accessRequestDto) throws EntityException {
        boolean isExternal = StringUtils.isNotBlank(accessRequestDto.getOrigin()) && !ProjectUser.REGARDS_ORIGIN.equals(
            accessRequestDto.getOrigin());
        return create(accessRequestDto, isExternal, UserStatus.ACCESS_GRANTED, AccountStatus.ACTIVE);
    }

    @Override
    public ProjectUser create(AccessRequestDto accessRequestDto,
                              boolean isExternal,
                              UserStatus userStatus,
                              AccountStatus accountStatus) throws EntityException {

        String email = accessRequestDto.getEmail();
        if (projectUserRepository.findOneByEmail(email).isPresent()) {
            throw new EntityAlreadyExistsException("The email address '" + email + "' is already in use.");
        }

        Account account = accountUtilsService.retrieveAccount(email);
        if (account != null) {
            LOG.info("Creating user with an existing account - no new account created");
        } else {
            account = accountUtilsService.createAccount(accessRequestDto, isExternal, accountStatus);
        }

        Role role;
        try {
            if (StringUtils.isNotBlank(accessRequestDto.getRoleName())) {
                role = roleService.retrieveRole(accessRequestDto.getRoleName());
            } else {
                role = roleService.retrieveRole(accessSettingsService.defaultRole());
            }
        } catch (EntityNotFoundException e) {
            LOG.debug(e.getMessage(), e);
            role = roleService.getDefaultRole();
            LOG.warn("Requested role does not exist : default role is set");
        }

        Set<String> accessGroups = new HashSet<>(accessSettingsService.defaultGroups());
        accessGroups.addAll(projectUserGroupService.getPublicGroups());
        Set<String> inputAccessGroups = accessRequestDto.getAccessGroups();
        if (!CollectionUtils.isEmpty(inputAccessGroups)) {
            projectUserGroupService.validateAccessGroups(inputAccessGroups, false);
            accessGroups.addAll(inputAccessGroups);
        }

        Long maxQuota = accessRequestDto.getMaxQuota() != null ?
            accessRequestDto.getMaxQuota() :
            quotaHelperService.getDefaultQuota();

        ProjectUser projectUser = new ProjectUser().setEmail(email)
                                                   .setLastName(account.getLastName())
                                                   .setFirstName(account.getFirstName())
                                                   .setRole(role)
                                                   .setAccessGroups(accessGroups)
                                                   .setMaxQuota(maxQuota);

        if (accessRequestDto.getMetadata() != null) {
            projectUser.setMetadata(new HashSet<>(accessRequestDto.getMetadata()));
        }

        if (isExternal) {
            projectUser.setOrigin(accessRequestDto.getOrigin());
            // External authenticated accounts don't need to validate email.
            projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        } else if (userStatus != null) {
            projectUser.setStatus(userStatus);
        }

        projectUser = save(projectUser);

        // Once project user is properly created, link project to account
        accountUtilsService.addProject(email);

        publisher.publish(new ProjectUserEvent(email, ProjectUserAction.CREATE));

        if (!CollectionUtils.isEmpty(accessSettingsService.userCreationMailRecipients())) {
            AccessRightsEmailWrapper wrapper = new AccessRightsEmailWrapper().setProjectUser(projectUser)
                                                                             .setSubject(
                                                                                 "[REGARDS] Project User created")
                                                                             .setTo(accessSettingsService.userCreationMailRecipients())
                                                                             .setTemplate(
                                                                                 AccessRightsTemplateConfiguration.USER_CREATED_TEMPLATE_NAME)
                                                                             .setDefaultMessage(
                                                                                 "User account created : "
                                                                                 + projectUser.getEmail());
            accessRightsEmailService.sendEmail(wrapper);
        }

        return projectUser;
    }

    /**
     * Specific on-save operations
     *
     * @param projectUser The user to save
     */
    private ProjectUser save(ProjectUser projectUser) {
        projectUser.setLastUpdate(OffsetDateTime.now());
        return projectUserRepository.save(projectUser);
    }

    @Override
    public void resetLicence() {
        List<ProjectUser> users = projectUserRepository.findAll();
        for (ProjectUser anyone : users) {
            anyone.setLicenseAccepted(false);
        }
        projectUserRepository.saveAll(users);
    }

    @Override
    public Collection<ProjectUser> retrieveUserByRole(Role role) {
        return projectUserRepository.findByRoleName(role.getName());
    }

    @Override
    public boolean canDelete(ProjectUser projectUser) {
        boolean canDelete = false;
        try {
            canDelete = roleService.isCurrentRoleSuperiorTo(projectUser.getRole().getName());
        } catch (EntityNotFoundException e) {
            LOG.warn("Invalid project user : {}", projectUser.getEmail());
        }
        return canDelete;
    }

    @Override
    public void sendVerificationEmail(String email) throws EntityNotFoundException {
        ProjectUser projectUser = retrieveOneByEmail(email);
        if (UserStatus.WAITING_EMAIL_VERIFICATION.equals(projectUser.getStatus())) {
            eventPublisher.publishEvent(new OnGrantAccessEvent(projectUser));
        }
    }

    @Override
    public void updateQuota(Map<String, Long> currentQuotaByEmail) {

        AtomicInteger counter = new AtomicInteger();
        int pageSize = 100;
        currentQuotaByEmail.keySet()
                           .stream()
                           .collect(Collectors.groupingBy(x -> counter.getAndIncrement() / pageSize))
                           .values()
                           .forEach(emailList -> projectUserRepository.findByEmailIn(emailList)
                                                                      .forEach(projectUser -> projectUser.setCurrentQuota(
                                                                          currentQuotaByEmail.get(projectUser.getEmail()))));
    }

    @Override
    public Map<String, Long> getUserCountByAccessGroup() {
        return projectUserRepository.getUserCountByAccessGroup();
    }

    @Override
    public void updateOrigin(String email, String origin) throws EntityNotFoundException {
        ProjectUser projectUser = retrieveOneByEmail(email);
        if (StringUtils.isNotBlank(origin)) {
            projectUser.setOrigin(origin);
        }
    }

}
