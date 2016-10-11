package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessRights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;

@Service
public class ProjectUserService implements IProjectUserService {

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IRoleRepository roleRepository;

    @Override
    public List<ProjectUser> retrieveUserList() {
        final Iterable<ProjectUser> projectUsers = projectUserRepository.findAll();
        return StreamSupport.stream(projectUsers.spliterator(), false).collect(Collectors.toList());
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
            throws OperationNotSupportedException {
        if (existUser(pUserId)) {
            if (pUpdatedProjectUser.getId() == pUserId) {
                projectUserRepository.save(pUpdatedProjectUser);
            }
            throw new OperationNotSupportedException("Account id specified differs from updated account id");
        }
        throw new NoSuchElementException(pUserId + "");

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
        final Function<Long, List<ResourcesAccess>> find = (id) -> {
            return pUpdatedUserAccessRights.stream().filter(e -> e.getId().equals(id)).collect(Collectors.toList());
        };
        final Function<Long, Boolean> contains = (id) -> {
            return !find.apply(id).isEmpty();
        };

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
            final String pBorrowedRoleName) throws OperationNotSupportedException {
        final ProjectUser projectUser = projectUserRepository.findOneByEmail(pLogin);
        final Role userRole = projectUser.getRole();
        Role returnedRole = userRole;

        if (pBorrowedRoleName != null) {
            final Role borrowedRole = roleRepository.findOneByName(pBorrowedRoleName);
            if (roleService.isHierarchicallyInferior(borrowedRole, returnedRole)) {
                returnedRole = borrowedRole;
            } else {
                throw new OperationNotSupportedException(
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
