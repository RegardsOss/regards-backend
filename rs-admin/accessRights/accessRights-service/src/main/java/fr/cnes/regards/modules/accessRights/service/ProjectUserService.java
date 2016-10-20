package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessRights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * {@link IProjectUserService} implementation
 *
 * @author CS SI
 */
@Service
public class ProjectUserService implements IProjectUserService {

    /**
     * CRUD repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Service handling CRUD operation on {@link Role}s
     */
    @Autowired
    private IRoleService roleService;

    /**
     * CRUD repository managing {@link Role}s. Autowired by Spring.
     */
    @Autowired
    private IRoleRepository roleRepository;

    @Override
    public List<ProjectUser> retrieveUserList() {
        try (Stream<ProjectUser> stream = StreamSupport.stream(projectUserRepository.findAll().spliterator(), true)) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public ProjectUser retrieveUser(final Long pUserId) {
        return projectUserRepository.findOne(pUserId);
    }

    @Override
    public ProjectUser retrieveUser(final String pLogin) {
        return projectUserRepository.findOneByEmail(pLogin);
    }

    @Override
    public void updateUser(final Long pUserId, final ProjectUser pUpdatedProjectUser)
            throws InvalidValueException, EntityNotFoundException {
        if (existUser(pUserId)) {
            if (pUpdatedProjectUser.getId() == pUserId) {
                projectUserRepository.save(pUpdatedProjectUser);
            }
            throw new InvalidValueException("Account id specified differs from updated account id");
        }
        throw new EntityNotFoundException(pUserId.toString(), ProjectUser.class);

    }

    @Override
    public void removeUser(final Long pUserId) {
        projectUserRepository.delete(pUserId);
    }

    @Override
    public void updateUserAccessRights(final String pLogin, final List<ResourcesAccess> pUpdatedUserAccessRights) {
        if (!existUser(pLogin)) {
            throw new NoSuchElementException("ProjectUser of given login (" + pLogin + ") could not be found");
        }
        final ProjectUser user = retrieveUser(pLogin);

        // Finder method
        // Pass the id and the list to search, returns the element with passed id
        final Function<Long, List<ResourcesAccess>> find = id -> pUpdatedUserAccessRights.stream()
                .filter(e -> e.getId().equals(id)).collect(Collectors.toList());
        final Function<Long, Boolean> contains = id -> !find.apply(id).isEmpty();

        final List<ResourcesAccess> permissions = user.getPermissions();
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        permissions.replaceAll(p -> contains.apply(p.getId()) ? find.apply(p.getId()).get(0) : p);
        projectUserRepository.save(user);
    }

    @Override
    public void removeUserAccessRights(final String pLogin) {
        final ProjectUser user = retrieveUser(pLogin);
        user.setPermissions(new ArrayList<>());
        projectUserRepository.save(user);
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
        projectUserRepository.save(user);
    }

    @Override
    public void removeUserMetaData(final Long pUserId) {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(new ArrayList<>());
        projectUserRepository.save(user);
    }

    @Override
    public Couple<List<ResourcesAccess>, Role> retrieveProjectUserAccessRights(final String pLogin,
            final String pBorrowedRoleName) throws InvalidValueException {
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

        return new Couple<>(projectUser.getPermissions(), returnedRole);

    }

    @Override
    public boolean existUser(final String pEmail) {
        final ProjectUser projectUser = projectUserRepository.findOneByEmail(pEmail);
        return projectUser != null;
    }

    @Override
    public boolean existUser(final Long pId) {
        return projectUserRepository.exists(pId);
    }

}
