/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

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
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveProjectUserList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> updateProjectUser(final Long pUserId, final ProjectUser pUpdatedProjectUser) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUser(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> updateProjectUserMetaData(final Long pUserId,
            final List<MetaData> pUpdatedUserMetaData) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUserMetaData(final Long pUserId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserAccessRights(final String pUserLogin,
            final String pBorrowedRoleName) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> updateProjectUserAccessRights(final String pLogin,
            final List<ResourcesAccess> pUpdatedUserAccessRights) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUserAccessRights(final String pUserLogin) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
