/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
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

    public ProjectUserService(IAuthenticationResolver authResolver, IProjectUserRepository pProjectUserRepository,
            final IRoleService pRoleService, IAccountsClient accountsClient,
            @Value("${regards.accounts.root.user.login}") String pInstanceAdminUserEmail, Gson gson) {
        super();
        this.authResolver = authResolver;
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        instanceAdminUserEmail = pInstanceAdminUserEmail;
        this.accountsClient = accountsClient;
        this.gson = gson;
    }

    @Override
    public Page<ProjectUser> retrieveUserList(String status, String emailStart, final Pageable pageable) {
        return projectUserRepository.findAll(ProjectUserSpecification.search(status, emailStart), pageable);
    }

    @Override
    public ProjectUser retrieveUser(final Long pUserId) throws EntityNotFoundException {
        final ProjectUser user = projectUserRepository.findOne(pUserId);
        // Check found
        if (user == null) {
            throw new EntityNotFoundException(pUserId.toString(), ProjectUser.class);
        }
        // Filter out hidden meta data
        try (final Stream<MetaData> stream = user.getMetadata().stream()) {
            user.setMetadata(stream.filter(keepVisibleMetaData).collect(Collectors.toList()));
        }
        return user;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.role.IProjectUserService#retrieveOneByEmail(java.lang.String)
     */
    @Override
    public ProjectUser retrieveOneByEmail(final String pUserEmail) throws EntityNotFoundException {
        final ProjectUser user;
        if (instanceAdminUserEmail.equals(pUserEmail)) {
            user = new ProjectUser(pUserEmail, new Role(DefaultRole.INSTANCE_ADMIN.toString(), null), new ArrayList<>(),
                    new ArrayList<>());
        } else {
            user = projectUserRepository.findOneByEmail(pUserEmail)
                    .orElseThrow(() -> new EntityNotFoundException(pUserEmail, ProjectUser.class));
            // Filter out hidden meta data
            try (final Stream<MetaData> stream = user.getMetadata().stream()) {
                stream.filter(keepVisibleMetaData);
            }
        }
        return user;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.role.IProjectUserService#retrieveCurrentUser()
     */
    @Override
    public ProjectUser retrieveCurrentUser() throws EntityNotFoundException {
        final String email = authResolver.getUser();
        return projectUserRepository.findOneByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Current user", ProjectUser.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#retrieveAccessRequestList()
     */
    @Override
    public Page<ProjectUser> retrieveAccessRequestList(final Pageable pPageable) {
        return projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS, pPageable);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#updateUser(java.lang.Long,
     * fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public ProjectUser updateUser(final Long pUserId, final ProjectUser pUpdatedProjectUser) throws EntityException {
        if (!pUpdatedProjectUser.getId().equals(pUserId)) {
            throw new EntityInconsistentIdentifierException(pUserId, pUpdatedProjectUser.getId(), ProjectUser.class);
        }
        if (!existUser(pUserId)) {
            throw new EntityNotFoundException(pUserId.toString(), ProjectUser.class);
        }

        return save(pUpdatedProjectUser);
    }

    @Override
    public ProjectUser updateUserInfos(final Long pUserId, final ProjectUser pUpdatedProjectUser)
            throws EntityException {

        if (!pUpdatedProjectUser.getId().equals(pUserId)) {
            throw new EntityInconsistentIdentifierException(pUserId, pUpdatedProjectUser.getId(), ProjectUser.class);
        }
        if (!existUser(pUserId)) {
            throw new EntityNotFoundException(pUserId.toString(), ProjectUser.class);
        }

        final ProjectUser user = projectUserRepository.findOne(pUserId);

        // Set user role
        if (pUpdatedProjectUser.getRole() == null) {
            user.setRole(null);
        } else if (pUpdatedProjectUser.getRole().getId() != null) {
            user.setRole(pUpdatedProjectUser.getRole());
        } else if (pUpdatedProjectUser.getRole().getName() != null) {
            final Role newRole = roleService.retrieveRole(pUpdatedProjectUser.getRole().getName());
            if (newRole != null) {
                user.setRole(newRole);
            } else {
                throw new EntityNotFoundException(pUpdatedProjectUser.getRole().getName(), Role.class);
            }
        }

        // Set user new metadata
        user.setMetadata(pUpdatedProjectUser.getMetadata());
        // Set user new permissions
        user.setPermissions(pUpdatedProjectUser.getPermissions());
        // Save new user informations
        return save(user);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#updateUserAccessRights(java.lang.
     * String, java.util.List)
     */
    @Override
    public void updateUserAccessRights(final String pLogin, final List<ResourcesAccess> pUpdatedUserAccessRights)
            throws EntityNotFoundException {
        final ProjectUser user = projectUserRepository.findOneByEmail(pLogin)
                .orElseThrow(() -> new EntityNotFoundException(pLogin, ProjectUser.class));

        try (final Stream<ResourcesAccess> previous = user.getPermissions().stream();
                final Stream<ResourcesAccess> updated = pUpdatedUserAccessRights.stream();
                final Stream<ResourcesAccess> merged = Stream.concat(updated, previous)) {
            user.setPermissions(merged.filter(RegardsStreamUtils.distinctByKey(r -> r.getId()))
                    .collect(Collectors.toList()));
        }

        save(user);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#removeUserAccessRights(java.lang.
     * String)
     */
    @Override
    public void removeUserAccessRights(final String pLogin) throws EntityNotFoundException {
        final ProjectUser user = projectUserRepository.findOneByEmail(pLogin)
                .orElseThrow(() -> new EntityNotFoundException(pLogin, ProjectUser.class));
        user.setPermissions(new ArrayList<>());
        save(user);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#retrieveUserMetaData(java.lang.Long)
     */
    @Override
    public List<MetaData> retrieveUserMetaData(final Long pUserId) throws EntityNotFoundException {
        final ProjectUser user = retrieveUser(pUserId);
        return user.getMetadata();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#updateUserMetaData(java.lang.Long,
     * java.util.List)
     */
    @Override
    public List<MetaData> updateUserMetaData(final Long pUserId, final List<MetaData> pUpdatedUserMetaData)
            throws EntityNotFoundException {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetadata(pUpdatedUserMetaData);
        final ProjectUser savedUser = save(user);
        return savedUser.getMetadata();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#removeUserMetaData(java.lang.Long)
     */
    @Override
    public void removeUserMetaData(final Long pUserId) throws EntityNotFoundException {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetadata(new ArrayList<>());
        save(user);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#retrieveProjectUserAccessRights(java
     * .lang.String, java.lang.String)
     */
    @Override
    public List<ResourcesAccess> retrieveProjectUserAccessRights(final String pEmail, final String pBorrowedRoleName)
            throws EntityException {
        final ProjectUser projectUser = retrieveOneByEmail(pEmail);
        final Role userRole = projectUser.getRole();
        Role returnedRole = userRole;

        if (pBorrowedRoleName != null) {
            final Role borrowedRole = roleService.retrieveRole(pBorrowedRoleName);
            if (roleService.isHierarchicallyInferior(borrowedRole, returnedRole)) {
                returnedRole = borrowedRole;
            } else {
                throw new EntityOperationForbiddenException(pBorrowedRoleName, Role.class,
                        "Borrowed role must be hierachically inferior to the project user's role");
            }
        }

        // Merge permissions from the project user and from the role
        final List<ResourcesAccess> merged = new ArrayList<>();
        final List<ResourcesAccess> fromUser = projectUser.getPermissions();
        merged.addAll(fromUser);
        try {
            final Set<ResourcesAccess> fromRole = roleService.retrieveRoleResourcesAccesses(returnedRole.getId());
            merged.addAll(fromRole);
        } catch (final EntityNotFoundException e) {
            LOG.debug("Could not retrieve permissions from role", e);
        }
        return merged;
    }

    @Override
    public ProjectUser createProjectUser(final AccessRequestDto pDto)
            throws EntityAlreadyExistsException, EntityInvalidException {
        try {
            ResponseEntity<Resource<Account>> accountResponse = accountsClient.retrieveAccounByEmail(pDto.getEmail());
            if (accountResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                Account newAccount = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(),
                        pDto.getPassword());
                newAccount.setStatus(AccountStatus.ACTIVE);
                AccountNPassword newAccountWithPassword = new AccountNPassword(newAccount, newAccount.getPassword());
                accountsClient.createAccount(newAccountWithPassword);
            }
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        }

        if (!existUser(pDto.getEmail())) {
            // Get role for projectUser to create
            Role role;
            try {
                if ((pDto.getRoleName() != null) && !pDto.getRoleName().isEmpty()) {
                    role = roleService.retrieveRole(pDto.getRoleName());
                } else {
                    role = roleService.getDefaultRole();
                }
            } catch (final EntityNotFoundException e) {
                role = roleService.getDefaultRole();
                LOG.warn("Request role does not exists, the new user is associated to default role");
                LOG.debug(e.getMessage(), e);
            }
            final ProjectUser newProjectUser = new ProjectUser();
            newProjectUser.setEmail(pDto.getEmail());
            newProjectUser.setRole(role);
            if (pDto.getMetadata() != null) {
                newProjectUser.setMetadata(pDto.getMetadata());
            }
            newProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
            return save(newProjectUser);
        } else {
            throw new EntityAlreadyExistsException("Project user already exists");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#existUser(java.lang.String)
     */
    @Override
    public boolean existUser(final String pEmail) {
        return projectUserRepository.findOneByEmail(pEmail).isPresent();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#existUser(java.lang.Long)
     */
    @Override
    public boolean existUser(final Long pId) {
        return projectUserRepository.exists(pId);
    }

    /**
     * Specific on-save operations
     * @param pProjectUser The user to save
     */
    private ProjectUser save(final ProjectUser pProjectUser) {
        pProjectUser.setLastUpdate(OffsetDateTime.now());
        return projectUserRepository.save(pProjectUser);
    }

    @Override
    public void resetLicence() {
        final List<ProjectUser> everyone = projectUserRepository.findAll();
        for (final ProjectUser anyone : everyone) {
            anyone.setLicenseAccepted(false);
        }
        projectUserRepository.save(everyone);
    }

    @Override
    public Collection<ProjectUser> retrieveUserByRole(Role role) {
        return projectUserRepository.findByRoleName(role.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService#delete(fr.cnes.regards.modules.
     * accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void deleteByEmail(String pEmail) throws EntityNotFoundException {
        ProjectUser projectUser = retrieveOneByEmail(pEmail);
        projectUserRepository.delete(projectUser);
    }

}
