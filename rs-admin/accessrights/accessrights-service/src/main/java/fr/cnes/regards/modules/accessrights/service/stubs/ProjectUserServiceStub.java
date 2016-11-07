/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.IRoleService;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * @author svissier
 *
 */
@Service
@Profile("test")
@Primary
public class ProjectUserServiceStub implements IProjectUserService {

    /**
     * Stub project users db
     */
    private List<ProjectUser> projectUsers;

    /**
     * CRUD repository managing {@link ProjectUser}s
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service handling CRUD operations on {@link Role}s
     */
    private final IRoleService roleService;

    /**
     * CRUD repository managing {@link Roles}s
     */
    private final IRoleRepository roleRepository;

    public ProjectUserServiceStub(final IProjectUserRepository pDaoProjectUser, final IRoleService pRoleService,
            final IRoleRepository pRoleRepository) {
        projectUserRepository = pDaoProjectUser;
        roleService = pRoleService;
        roleRepository = pRoleRepository;
        try (Stream<ProjectUser> stream = StreamSupport.stream(projectUserRepository.findAll().spliterator(), false)) {
            projectUsers = stream.collect(Collectors.toList());
        }
    }

    /*
     * (non-Javadoc)
     * fr.cnes.regards.modules.accessrights.service.IProjectUserServicee.IProjectUserService#retrieveUserList()
     */
    @Override
    public List<ProjectUser> retrieveUserList() {
        return projectUsers.stream().filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadocfr.cnes.regards.modules.accessrights.service.IProjectUserService.service.IProjectUserService#
     * retrieveUser(int)
     */
    @Override
    public ProjectUser retrieveUser(final Long pUserId) {
        final List<ProjectUser> notWaitingAccess = projectUsers.stream()
                .filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCESS)).collect(Collectors.toList());
        final ProjectUser wanted = notWaitingAccess.stream().filter(p -> p.getId() == pUserId).findFirst().get();
        final List<MetaData> visible = wanted.getMetaData().stream()
                .filter(m -> !m.getVisibility().equals(UserVisibility.HIDDEN)).collect(Collectors.toList());
        return new ProjectUser(wanted.getId(), wanted.getLastConnection(), wanted.getLastUpdate(), wanted.getStatus(),
                visible, wanted.getRole(), wanted.getPermissions(), wanted.getEmail());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IProjectUserService#retrieveOneByEmail(java.lang.String)
     */
    @Override
    public ProjectUser retrieveOneByEmail(final String pUserEmail) throws EntityNotFoundException {
        for (final ProjectUser projectUser : projectUsers) {
            if (projectUser.getEmail().equals(pUserEmail)) {
                return projectUser;
            }
        }
        throw new EntityNotFoundException(pUserEmail, ProjectUser.class);
    }

    @Override
    public void updateUser(final Long pUserId, final ProjectUser pUpdatedProjectUser)
            throws InvalidValueException, EntityNotFoundException {
        if (!existUser(pUserId)) {
            throw new EntityNotFoundException(pUserId.toString(), ProjectUser.class);
        }
        if (!pUserId.equals(pUpdatedProjectUser.getId())) {
            throw new InvalidValueException("Account id specified differs from updated account id");
        }

        try (Stream<ProjectUser> stream = projectUsers.stream()) {

            final Function<ProjectUser, ProjectUser> replaceWithUpdated = p -> {
                ProjectUser result = null;
                if (p.getId().equals(pUserId)) {
                    result = pUpdatedProjectUser;
                } else {
                    result = p;
                }
                return result;
            };
            projectUsers = stream.map(replaceWithUpdated).collect(Collectors.toList());
        }
    }

    /**
     * @param pUserId
     * @return
     */
    @Override
    public boolean existUser(final Long pUserId) {
        return projectUsers.stream().filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .filter(p -> p.getId() == pUserId).findFirst().isPresent();
    }

    @Override
    public boolean existUser(final String pEmail) {
        final ProjectUser projectUser = projectUserRepository.findOneByEmail(pEmail);
        return projectUser != null;
    }

    /*
     * fr.cnes.regards.modules.accessrights.service.IProjectUserServices.accessrights.service.IProjectUserService#
     * removeUser(int)
     */
    @Override
    public void removeUser(final Long pUserId) {
        projectUsers.stream().filter(p -> p.getId() != pUserId).collect(Collectors.toList());
    }

    @Override
    public List<ResourcesAccess> retrieveProjectUserAccessRights(final String pLogin, final String pBorrowedRoleName)
            throws InvalidValueException {
        final ProjectUser projectUser = projectUserRepository.findOneByEmail(pLogin);
        final Role userRole = projectUser.getRole();
        Role returnedRole = userRole;

        if (pBorrowedRoleName != null) {
            final Role borrowedRole = roleRepository.findOneByName(pBorrowedRoleName);
            if (roleService.isHierarchicallyInferior(borrowedRole, returnedRole)) {
                returnedRole = borrowedRole;
            } else {
                throw new InvalidValueException(
                        "Borrowed role must be hierachically inferior to the project user's role");
            }
        }

        return projectUser.getPermissions();
    }

    @Override
    public void updateUserAccessRights(final String pLogin, final List<ResourcesAccess> pUpdatedUserAccessRights) {
        if (!existUser(pLogin)) {
            throw new NoSuchElementException("ProjectUser of given login (" + pLogin + ") could not be found");
        }
        final ProjectUser user = projectUserRepository.findOneByEmail(pLogin);

        // Finder method
        // Pass the id and the list to search, returns the element with passed id
        final Function<Long, List<ResourcesAccess>> find = id -> pUpdatedUserAccessRights.stream()
                .filter(e -> e.getId().equals(id)).collect(Collectors.toList());

        final Function<Long, Boolean> contains = id -> !find.apply(id).isEmpty();

        final List<ResourcesAccess> permissions = user.getPermissions();
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        permissions.replaceAll(p -> contains.apply(p.getId()) ? find.apply(p.getId()).get(0) : p);
    }

    @Override
    public void removeUserAccessRights(final String pLogin) {
        final ProjectUser user = projectUserRepository.findOneByEmail(pLogin);
        user.setPermissions(new ArrayList<>());
    }

    @Override
    public List<MetaData> retrieveUserMetaData(final Long pUserId) {
        final ProjectUser user = retrieveUser(pUserId);
        return user.getMetaData();
    }

    @Override
    public void updateUserMetaData(final Long pUserId, final List<MetaData> pUpdatedUserMetaData) {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(pUpdatedUserMetaData);
    }

    @Override
    public void removeUserMetaData(final Long pUserId) {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(new ArrayList<>());

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IProjectUserService#retrieveCurrentUser()
     */
    @Override
    public ProjectUser retrieveCurrentUser() {
        return retrieveUserList().get(0);
    }

}
