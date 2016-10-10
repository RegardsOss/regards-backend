/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessRights.client.ProjectUsersClient;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;

@Component
public class ProjectUsersFallback implements ProjectUsersClient {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersFallback.class);

    private static final String fallBackErrorMessage = "RS-ADMIN /users request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveProjectUserList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectUser>> retrieveProjectUser(Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUser(Long pUserId, ProjectUser pUpdatedProjectUser)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUser(Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUserMetaData(Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<Couple<List<ResourcesAccess>, Role>>> retrieveProjectUserAccessRights(String pUserLogin,
            String pBorrowedRoleName) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUserAccessRights(String pUserLogin,
            List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUserAccessRights(String pUserLogin) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
