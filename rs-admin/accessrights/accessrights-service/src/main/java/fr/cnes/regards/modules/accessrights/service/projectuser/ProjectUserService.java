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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.gson.Gson;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.ProjectUserSpecification;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.RegardsStreamUtils;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;

/**
 * {@link IProjectUserService} implementation
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class ProjectUserService implements IProjectUserService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUserService.class);

    /**
     * CRUD repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service handling CRUD operation on {@link Role}s
     */
    private final IRoleService roleService;

    /**
     * Account service used to manage accounts.
     */
    private final IAccountsClient accountsClient;

    /**
     * Authentication resolver
     */
    private final IAuthenticationResolver authResolver;

    private final IAccessSettingsService accessSettingsService;

    private final IUserClient userAccessGroupsClient;

    /**
     * Gson serializer/deserializer
     */
    private final Gson gson;

    /**
     * A filter on meta data to keep visible ones only
     */
    private final Predicate<? super MetaData> keepVisibleMetaData = m -> !UserVisibility.HIDDEN
            .equals(m.getVisibility());

    /**
     * Configured instance administrator user email/login
     */
    private final String instanceAdminUserEmail;

    public ProjectUserService(IAuthenticationResolver authResolver, IProjectUserRepository projectUserRepository,
            final IRoleService roleService, IAccountsClient accountsClient, IUserClient userAccessGroupsClient,
            @Value("${regards.accounts.root.user.login}") String instanceAdminUserEmail,
            IAccessSettingsService accessSettingsService, Gson gson) {
        super();
        this.authResolver = authResolver;
        this.projectUserRepository = projectUserRepository;
        this.roleService = roleService;
        this.instanceAdminUserEmail = instanceAdminUserEmail;
        this.accountsClient = accountsClient;
        this.userAccessGroupsClient = userAccessGroupsClient;
        this.accessSettingsService = accessSettingsService;
        this.gson = gson;
    }

    @Override
    public Page<ProjectUser> retrieveUserList(String status, String emailStart, final Pageable pageable) {
        return projectUserRepository.findAll(ProjectUserSpecification.search(status, emailStart), pageable);
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
        user.setMetadata(user.getMetadata().stream().filter(keepVisibleMetaData).collect(Collectors.toList()));
        return user;
    }

    @Override
    public ProjectUser retrieveOneByEmail(String userEmail) throws EntityNotFoundException {
        return retrieveOneOptionalByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException(userEmail, ProjectUser.class));
    }

    @Override
    public Optional<ProjectUser> retrieveOneOptionalByEmail(String userEmail) {
        Optional<ProjectUser> user;
        if (instanceAdminUserEmail.equals(userEmail)) {
            user = Optional.of(new ProjectUser(userEmail, new Role(DefaultRole.INSTANCE_ADMIN.toString(), null),
                    new ArrayList<>(), new ArrayList<>()));
        } else {
            user = projectUserRepository.findOneByEmail(userEmail);
            // Filter out hidden meta data
            if (user.isPresent()) {
                try (Stream<MetaData> stream = user.get().getMetadata().stream()) {
                    user.get().setMetadata(stream.filter(keepVisibleMetaData).collect(Collectors.toList()));
                }
            }
        }
        return user;
    }

    @Override
    public ProjectUser retrieveCurrentUser() throws EntityNotFoundException {
        String email = authResolver.getUser();
        return projectUserRepository.findOneByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Current user", ProjectUser.class));
    }

    @Override
    public Page<ProjectUser> retrieveAccessRequestList(Pageable pageable) {
        return projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS, pageable);
    }

    @Override
    public ProjectUser updateUser(Long userId, ProjectUser updatedProjectUser) throws EntityException {
        if (!updatedProjectUser.getId().equals(userId)) {
            throw new EntityInconsistentIdentifierException(userId, updatedProjectUser.getId(), ProjectUser.class);
        }
        if (!existUser(userId)) {
            throw new EntityNotFoundException(userId.toString(), ProjectUser.class);
        }

        return save(updatedProjectUser);
    }

    @Override
    public ProjectUser updateUserInfos(Long userId, ProjectUser updatedProjectUser) throws EntityException {

        if (!updatedProjectUser.getId().equals(userId)) {
            throw new EntityInconsistentIdentifierException(userId, updatedProjectUser.getId(), ProjectUser.class);
        }
        if (!existUser(userId)) {
            throw new EntityNotFoundException(userId.toString(), ProjectUser.class);
        }

        ProjectUser user = projectUserRepository.findById(userId).get();

        // Set user role
        if (updatedProjectUser.getRole() == null) {
            user.setRole(null);
        } else if (updatedProjectUser.getRole().getId() != null) {
            user.setRole(updatedProjectUser.getRole());
        } else if (updatedProjectUser.getRole().getName() != null) {
            Role newRole = roleService.retrieveRole(updatedProjectUser.getRole().getName());
            if (newRole != null) {
                user.setRole(newRole);
            } else {
                throw new EntityNotFoundException(updatedProjectUser.getRole().getName(), Role.class);
            }
        }

        // Set user new metadata
        user.setMetadata(updatedProjectUser.getMetadata());
        // Set user new permissions
        user.setPermissions(updatedProjectUser.getPermissions());
        // Save new user informations
        return save(user);
    }

    @Override
    public void updateUserAccessRights(String login, List<ResourcesAccess> updatedUserAccessRights)
            throws EntityNotFoundException {
        ProjectUser user = projectUserRepository.findOneByEmail(login)
                .orElseThrow(() -> new EntityNotFoundException(login, ProjectUser.class));

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
                .orElseThrow(() -> new EntityNotFoundException(login, ProjectUser.class));
        user.setPermissions(new ArrayList<>());
        save(user);
    }

    @Override
    public List<MetaData> retrieveUserMetaData(Long userId) throws EntityNotFoundException {
        ProjectUser user = retrieveUser(userId);
        return user.getMetadata();
    }

    @Override
    public List<MetaData> updateUserMetaData(Long userId, List<MetaData> updatedUserMetaData)
            throws EntityNotFoundException {
        ProjectUser user = retrieveUser(userId);
        user.setMetadata(updatedUserMetaData);
        ProjectUser savedUser = save(user);
        return savedUser.getMetadata();
    }

    @Override
    public void removeUserMetaData(Long userId) throws EntityNotFoundException {
        ProjectUser user = retrieveUser(userId);
        user.setMetadata(new ArrayList<>());
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
                throw new EntityOperationForbiddenException(borrowedRoleName, Role.class,
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
    public ProjectUser createProjectUser(AccessRequestDto accessRequestDto)
            throws EntityAlreadyExistsException, EntityInvalidException {
        try {
            ResponseEntity<EntityModel<Account>> accountResponse = accountsClient
                    .retrieveAccounByEmail(accessRequestDto.getEmail());
            if (accountResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                Account newAccount = new Account(accessRequestDto.getEmail(), accessRequestDto.getFirstName(),
                        accessRequestDto.getLastName(), accessRequestDto.getPassword());
                newAccount.setStatus(AccountStatus.ACTIVE);
                AccountNPassword newAccountWithPassword = new AccountNPassword(newAccount, newAccount.getPassword());
                accountsClient.createAccount(newAccountWithPassword);
            }
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        }

        if (!existUser(accessRequestDto.getEmail())) {
            AccessSettings settings = accessSettingsService.retrieve();
            // Get role for projectUser to create
            Role role;
            try {
                if ((accessRequestDto.getRoleName() != null) && !accessRequestDto.getRoleName().isEmpty()) {
                    role = roleService.retrieveRole(accessRequestDto.getRoleName());
                } else {
                    role = settings.getDefaultRole();
                }
            } catch (EntityNotFoundException e) {
                role = roleService.getDefaultRole();
                LOG.warn("Request role does not exists, the new user is associated to default role");
                LOG.debug(e.getMessage(), e);
            }
            ProjectUser newProjectUser = new ProjectUser();
            newProjectUser.setEmail(accessRequestDto.getEmail());
            newProjectUser.setRole(role);
            if (accessRequestDto.getMetadata() != null) {
                newProjectUser.setMetadata(accessRequestDto.getMetadata());
            }
            newProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
            return save(newProjectUser);
        } else {
            throw new EntityAlreadyExistsException("Project user already exists");
        }
    }

    @Override
    public boolean existUser(String email) {
        return projectUserRepository.findOneByEmail(email).isPresent();
    }

    @Override
    public boolean existUser(Long id) {
        return projectUserRepository.existsById(id);
    }

    /**
     * Specific on-save operations
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
    public void deleteByEmail(String Email) throws EntityNotFoundException {
        ProjectUser projectUser = retrieveOneByEmail(Email);
        projectUserRepository.delete(projectUser);
    }

    @Override
    public void configureAccessGroups(ProjectUser projectUser) {
        AccessSettings settings = accessSettingsService.retrieve();
        if ((settings != null) && (settings.getDefaultGroups() != null) && !settings.getDefaultGroups().isEmpty()) {
            settings.getDefaultGroups().forEach(group -> {
                try {
                    userAccessGroupsClient.associateAccessGroupToUser(projectUser.getEmail(), group);
                } catch (HttpServerErrorException | HttpClientErrorException e) {
                    LOG.error(String.format("Error associating group %s to user %s.", group, projectUser.getEmail()),
                              e);
                }
            });
        }
    }

}
