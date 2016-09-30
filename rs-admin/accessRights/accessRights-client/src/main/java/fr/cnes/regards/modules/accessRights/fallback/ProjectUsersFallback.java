/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import fr.cnes.regards.modules.accessRights.client.ProjectUsersClient;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

public class ProjectUsersFallback implements ProjectUsersClient {

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveProjectUserList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectUser>> retrieveProjectUser(Long pUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUser(Long pUserId, ProjectUser pUpdatedProjectUser)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUser(Long pUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(Long pUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUserMetaData(Long pUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Couple<List<ResourcesAccess>, Role>>> retrieveProjectUserAccessRights(String pUserLogin,
            String pBorrowedRoleName) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUserAccessRights(String pUserLogin,
            List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUserAccessRights(String pUserLogin) {
        // TODO Auto-generated method stub
        return null;
    }

}
