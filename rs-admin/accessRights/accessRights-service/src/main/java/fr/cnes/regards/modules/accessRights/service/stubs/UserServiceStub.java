/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.IDaoProjectUser;
import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.IProjectUser;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.domain.UserVisibility;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.service.IUserService;

/**
 * @author svissier
 *
 */
@Service
@Profile("test")
@Primary
public class UserServiceStub implements IUserService {

    public static List<ProjectUser> projectUsers_;

    private final IDaoProjectUser daoProjectUser_;

    private final IRoleService roleService_;

    private final IRoleRepository roleRepository_;

    public UserServiceStub(IDaoProjectUser pDaoProjectUser, IRoleService pRoleService,
            IRoleRepository pRoleRepository) {
        daoProjectUser_ = pDaoProjectUser;
        roleService_ = pRoleService;
        roleRepository_ = pRoleRepository;
        projectUsers_ = daoProjectUser_.getAll();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUserList()
     */
    @Override
    public List<ProjectUser> retrieveUserList() {
        return projectUsers_.stream().filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCES))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUser(int)
     */
    @Override
    public ProjectUser retrieveUser(Long pUserId) {
        List<ProjectUser> notWaitingAccess = projectUsers_.stream()
                .filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCES)).collect(Collectors.toList());
        ProjectUser wanted = notWaitingAccess.stream().filter(p -> p.getId() == pUserId).findFirst().get();
        List<MetaData> visible = wanted.getMetaData().stream()
                .filter(m -> !m.getVisibility().equals(UserVisibility.HIDDEN)).collect(Collectors.toList());
        ProjectUser sent = new ProjectUser(wanted.getId(), wanted.getLastConnection(), wanted.getLastUpdate(),
                wanted.getStatus(), visible, wanted.getRole(), wanted.getPermissions(), wanted.getAccount());
        return sent;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#updateUser(int,
     * fr.cnes.regards.modules.accessRights.domain.ProjectUser)
     */
    @Override
    public void updateUser(Long pUserId, ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException {
        if (existUser(pUserId)) {
            if (pUpdatedProjectUser.getId() == pUserId) {
                projectUsers_ = projectUsers_.stream().map(a -> a.getId() == pUserId ? pUpdatedProjectUser : a)
                        .collect(Collectors.toList());
                return;
            }
            throw new OperationNotSupportedException("Account id specified differs from updated account id");
        }
        throw new NoSuchElementException(pUserId + "");
    }

    /**
     * @param pUserId
     * @return
     */
    public boolean existUser(Long pUserId) {
        return projectUsers_.stream().filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getId() == pUserId).findFirst().isPresent();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#removeUser(int)
     */
    @Override
    public void removeUser(Long pUserId) {
        projectUsers_ = projectUsers_.stream().filter(p -> p.getId() != pUserId).collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUserAccessRights(int)
     */
    @Override
    public Couple<List<ResourcesAccess>, Role> retrieveUserAccessRights(Long pUserId, String pBorrowedRoleName)
            throws OperationNotSupportedException {
        IProjectUser user = retrieveUser(pUserId);
        Role userRole = user.getRole();
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

        return new Couple<>(user.getPermissions(), returnedRole);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#updateUserAccessRights(int,
     * fr.cnes.regards.modules.accessRights.domain.ProjectUser)
     */
    @Override
    public void updateUserAccessRights(Long pUserId, List<ResourcesAccess> pUpdatedUserAccessRights) {
        if (!existUser(pUserId)) {
            throw new NoSuchElementException("ProjectUser of given id (" + pUserId + ") could not be found");
        }
        ProjectUser user = retrieveUser(pUserId);

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

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#removeUserAccessRights(int)
     */
    @Override
    public void removeUserAccessRights(Long pUserId) {
        ProjectUser user = retrieveUser(pUserId);
        user.setPermissions(new ArrayList<>());
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
    }

    @Override
    public void removeUserMetaData(Long pUserId) {
        ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(new ArrayList<>());

    }

}
