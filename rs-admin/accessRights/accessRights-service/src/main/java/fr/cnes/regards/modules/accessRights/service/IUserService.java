/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

/**
 * @author svissier
 *
 */
public interface IUserService {

    /**
     * @return
     */
    List<ProjectUser> retrieveUserList();

    /**
     * @param pUserId
     * @return
     */
    ProjectUser retrieveUser(int pUserId);

    /**
     * @param pUserId
     * @param pUpdatedProjectUser
     * @return
     * @throws OperationNotSupportedException
     */
    void updateUser(int pUserId, ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException;

    /**
     * @param pUserId
     * @return
     */
    void removeUser(int pUserId);

    /**
     * @param pUserId
     * @return
     */
    Couple<List<ResourcesAccess>, Role> retrieveUserAccessRights(int pUserId);

    /**
     * @param pUserId
     * @param pUpdatedProjectUser
     */
    void updateUserAccessRights(int pUserId, List<ResourcesAccess> pUpdatedUserAccessRights);

    /**
     * @param pUserId
     */
    void removeUserAccessRights(int pUserId);

}
