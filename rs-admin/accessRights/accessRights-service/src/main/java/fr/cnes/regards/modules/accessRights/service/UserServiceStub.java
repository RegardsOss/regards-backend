/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.IDaoProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;

/**
 * @author svissier
 *
 */
@Service
public class UserServiceStub implements IUserService {

    private static List<ProjectUser> projectUsers_;

    @Autowired
    private IDaoProjectUser projectUserDao_;

    @PostConstruct
    public void init() {
        projectUsers_ = projectUserDao_.getAll();

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUserList()
     */
    @Override
    public List<ProjectUser> retrieveUserList() {
        return projectUsers_.stream().filter(p->!p.getStatus().equals(UserStatus.WAITING_ACCES)).collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUser(int)
     */
    @Override
    public ProjectUser retrieveUser(int pUserId) {
    	List<ProjectUser> notWaitingAccess=projectUsers_.stream().filter(p->!p.getStatus().equals(UserStatus.WAITING_ACCES)).collect(Collectors.toList());
        return notWaitingAccess.stream()
                .filter(p -> p.getProjectUserId() == pUserId).findFirst().get();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#updateUser(int,
     * fr.cnes.regards.modules.accessRights.domain.ProjectUser)
     */
    @Override
    public void updateUser(int pUserId, ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException {
        if (existUser(pUserId)) {
            if (pUpdatedProjectUser.getProjectUserId() == pUserId) {
                projectUsers_ = projectUsers_.stream().map(a -> a.getProjectUserId() == pUserId ? pUpdatedProjectUser : a)
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
    public boolean existUser(int pUserId) {
        return projectUsers_.stream().filter(p -> !p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getProjectUserId() == pUserId).findFirst().isPresent();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#removeUser(int)
     */
    @Override
    public void removeUser(int pUserId) {
        projectUsers_ = projectUsers_.stream().filter(p -> p.getProjectUserId() != pUserId).collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUserAccessRights(int)
     */
    @Override
    public Couple<List<ResourcesAccess>, Role> retrieveUserAccessRights(int pUserId) {
        ProjectUser user = this.retrieveUser(pUserId);
        Role userRole = user.getRole();
        return new Couple<>(user.getPermissions(), userRole);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#updateUserAccessRights(int,
     * fr.cnes.regards.modules.accessRights.domain.ProjectUser)
     */
    @Override
    public void updateUserAccessRights(int pUserId, List<ResourcesAccess> pUpdatedUserAccessRights) {
        if (!existUser(pUserId)) {
            throw new NoSuchElementException("ProjectUser of given id (" + pUserId + ") could not be found");
        }
        ProjectUser user = this.retrieveUser(pUserId);

        // Finder method
        // Pass the id and the list to search, returns the element with passed id
        Function<Integer, List<ResourcesAccess>> find = (id) -> {
            return pUpdatedUserAccessRights.stream().filter(e -> e.getResourcesAccessId().equals(id))
                    .collect(Collectors.toList());
        };
        Function<Integer, Boolean> contains = (id) -> {
            return !find.apply(id).isEmpty();
        };

        List<ResourcesAccess> permissions = user.getPermissions();
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        permissions.replaceAll(p -> contains.apply(p.getResourcesAccessId())
                ? find.apply(p.getResourcesAccessId()).get(0) : p);

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#removeUserAccessRights(int)
     */
    @Override
    public void removeUserAccessRights(int pUserId) {
        ProjectUser user = this.retrieveUser(pUserId);
        user.setPermissions(new ArrayList<>());
    }

}
