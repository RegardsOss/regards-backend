/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
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
    ProjectUser retrieveUser(Long pUserId);

    /**
     * @param pUserId
     * @param pUpdatedProjectUser
     * @return
     * @throws OperationNotSupportedException
     */
    void updateUser(Long pUserId, ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException;

    /**
     * @param pUserId
     * @return
     */
    void removeUser(Long pUserId);

    /**
     * @param pUserId
     * @param pBorrowedRoleName
     * @return
     * @throws OperationNotSupportedException
     */
    Couple<List<ResourcesAccess>, Role> retrieveUserAccessRights(Long pUserId, String pBorrowedRoleName)
            throws OperationNotSupportedException;

    /**
     * @param pUserId
     * @param pUpdatedProjectUser
     */
    void updateUserAccessRights(Long pUserId, List<ResourcesAccess> pUpdatedUserAccessRights);

    /**
     * @param pUserId
     */
    void removeUserAccessRights(Long pUserId);

    List<MetaData> retrieveUserMetaData(Long pUserId);

    void updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData);

    void removeUserMetaData(Long pUserId);

}
