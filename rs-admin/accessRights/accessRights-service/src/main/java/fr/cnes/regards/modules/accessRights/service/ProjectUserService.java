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

import fr.cnes.regards.modules.accessRights.dao.IProjectUserRepository;
import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

@Service
public class ProjectUserService implements IProjectUserService {

    @Autowired
    private IProjectUserRepository projectUserRepository_;

    @Autowired
    private IRoleService roleService_;

    @Autowired
    private IRoleRepository roleRepository_;

    @Override
    public List<ProjectUser> retrieveUserList() {
        Iterable<ProjectUser> projectUsers = projectUserRepository_.findAll();
        return StreamSupport.stream(projectUsers.spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public ProjectUser retrieveUser(Long pUserId) {
        return projectUserRepository_.findOne(pUserId);
    }

    @Override
    public ProjectUser retrieveUser(String pLogin) {
        return projectUserRepository_.findOneByLogin(pLogin);
    }

    @Override
    public void updateUser(Long pUserId, ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException {
        if (existUser(pUserId)) {
            if (pUpdatedProjectUser.getId() == pUserId) {
                projectUserRepository_.save(pUpdatedProjectUser);
            }
            throw new OperationNotSupportedException("Account id specified differs from updated account id");
        }
        throw new NoSuchElementException(pUserId + "");

    }

    @Override
    public void removeUser(Long pUserId) {
        projectUserRepository_.delete(pUserId);
    }

    @Override
    public void updateUserAccessRights(String pLogin, List<ResourcesAccess> pUpdatedUserAccessRights) {
        if (!existUser(pLogin)) {
            throw new NoSuchElementException("ProjectUser of given login (" + pLogin + ") could not be found");
        }
        ProjectUser user = retrieveUser(pLogin);

        // Finder method
        // Pass the id and the list to search, returns the element with passed id
        Function<Long, List<ResourcesAccess>> find = (id) -> {
            return pUpdatedUserAccessRights.stream().filter(e -> e.getId().equals(id)).collect(Collectors.toList());
        };
        Function<Long, Boolean> contains = (id) -> {
            return !find.apply(id).isEmpty();
        };

        List<ResourcesAccess> permissions = user.getPermissions();
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        permissions.replaceAll(p -> contains.apply(p.getId()) ? find.apply(p.getId()).get(0) : p);
        projectUserRepository_.save(user);
    }

    @Override
    public void removeUserAccessRights(String pLogin) {
        ProjectUser user = retrieveUser(pLogin);
        user.setPermissions(new ArrayList<>());
        projectUserRepository_.save(user);
    }

    @Override
    public List<MetaData> retrieveUserMetaData(Long pUserId) {
        ProjectUser user = retrieveUser(pUserId);
        return user.getMetaData();
    }

    @Override
    public void updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData) {
        ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(pUpdatedUserMetaData);
        projectUserRepository_.save(user);
    }

    @Override
    public void removeUserMetaData(Long pUserId) {
        ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(new ArrayList<>());
        projectUserRepository_.save(user);
    }

    @Override
    public Couple<List<ResourcesAccess>, Role> retrieveProjectUserAccessRights(String pLogin, String pBorrowedRoleName)
            throws OperationNotSupportedException {
        ProjectUser projectUser = projectUserRepository_.findOneByLogin(pLogin);
        Role userRole = projectUser.getRole();
        Role returnedRole = userRole;

        if (pBorrowedRoleName != null) {
            Role borrowedRole = roleRepository_.findOneByName(pBorrowedRoleName);
            if (roleService_.isHierarchicallyInferior(borrowedRole, returnedRole)) {
                returnedRole = borrowedRole;
            }
            else {
                throw new OperationNotSupportedException(
                        "Borrowed role must be hierachically inferior to the project user's role");
            }
        }

        return new Couple<>(projectUser.getPermissions(), returnedRole);

    }

    @Override
    public boolean existUser(String pLogin) {
        return projectUserRepository_.exists(pLogin);
    }

    @Override
    public boolean existUser(Long pId) {
        return projectUserRepository_.exists(pId);
    }

}
