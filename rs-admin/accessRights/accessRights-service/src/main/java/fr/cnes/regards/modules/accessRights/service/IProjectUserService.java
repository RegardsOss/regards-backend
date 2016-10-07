/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;

/**
 * @author svissier
 *
 */
public interface IProjectUserService {

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
     * @param pUserLogin
     * @return
     */
    ProjectUser retrieveUser(String pLogin);

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
     * @param pUserLogin
     * @param pBorrowedRoleName
     * @return
     * @throws OperationNotSupportedException
     */
    Couple<List<ResourcesAccess>, Role> retrieveProjectUserAccessRights(String pLogin, String pBorrowedRoleName)
            throws OperationNotSupportedException;

    /**
     * @param pLogin
     * @param pUpdatedProjectUser
     */
    void updateUserAccessRights(String pLogin, List<ResourcesAccess> pUpdatedUserAccessRights);

    /**
     * @param pLogin
     */
    void removeUserAccessRights(String pLogin);

    List<MetaData> retrieveUserMetaData(Long pUserId);

    void updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData);

    void removeUserMetaData(Long pUserId);

    boolean existUser(Long pId);

    boolean existUser(String pLogin);
}
