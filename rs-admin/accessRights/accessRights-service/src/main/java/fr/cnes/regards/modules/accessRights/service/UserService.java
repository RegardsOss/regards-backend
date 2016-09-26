package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

@Service
public class UserService implements IUserService {

    @Override
    public List<ProjectUser> retrieveUserList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectUser retrieveUser(Long pUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateUser(Long pUserId, ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeUser(Long pUserId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Couple<List<ResourcesAccess>, Role> retrieveUserAccessRights(Long pUserId, String pBorrowedRoleName)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateUserAccessRights(Long pUserId, List<ResourcesAccess> pUpdatedUserAccessRights) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeUserAccessRights(Long pUserId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<MetaData> retrieveUserMetaData(Long pUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeUserMetaData(Long pUserId) {
        // TODO Auto-generated method stub

    }

}
