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

import fr.cnes.regards.modules.accessRights.client.IRolesClient;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

/**
 *
 * Class RolesFallback
 *
 * Fallback for Roles Feign client. This implementation is used in case of error during feign client calls.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class RolesFallback implements IRolesClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersFallback.class);

    /**
     * Common error message to log
     */
    private static final String fallBackErrorMessage = "RS-ADMIN /roles request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<Role>>> retrieveRoleList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<Role>> createRole(final Role pNewRole) throws AlreadyExistingException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<Role>> retrieveRole(final Long pRoleId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateRole(final Long pRoleId, final Role pUpdatedRole)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeRole(final Long pRoleId) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(final Long pRoleId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateRoleResourcesAccess(final Long pRoleId,
            final List<ResourcesAccess> pResourcesAccessList) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> clearRoleResourcesAccess(final Long pRoleId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(final Long pRoleId)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
