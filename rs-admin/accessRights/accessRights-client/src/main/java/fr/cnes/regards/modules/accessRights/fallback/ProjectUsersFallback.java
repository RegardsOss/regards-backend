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

import fr.cnes.regards.modules.accessRights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;

/**
 *
 * Class ProjectUsersFallback
 *
 * Fallback for ProjectUsers Feign client. This implementation is used in case of error during feign client calls.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class ProjectUsersFallback implements IProjectUsersClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersFallback.class);

    /**
     * Common error message to log
     */
    private static final String fallBackErrorMessage = "RS-ADMIN /users request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveProjectUserList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectUser>> retrieveProjectUser(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUser(final Long pUserId, final ProjectUser pUpdatedProjectUser)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUser(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUserMetaData(final Long pUserId, final List<MetaData> pUpdatedUserMetaData)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUserMetaData(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<Couple<List<ResourcesAccess>, Role>>> retrieveProjectUserAccessRights(
            final String pUserLogin, final String pBorrowedRoleName) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateProjectUserAccessRights(final String pUserLogin,
            final List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeProjectUserAccessRights(final String pUserLogin) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
