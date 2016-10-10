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

import fr.cnes.regards.modules.accessRights.client.RolesClient;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Component
public class RolesFallback implements RolesClient {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersFallback.class);

    private static final String fallBackErrorMessage = "RS-ADMIN /roles request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<Role>>> retrieveRoleList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<Role>> createRole(Role pNewRole) throws AlreadyExistingException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Resource<Role>> retrieveRole(Long pRoleId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateRole(Long pRoleId, Role pUpdatedRole) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> removeRole(Long pRoleId) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(Long pRoleId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<Void> clearRoleResourcesAccess(Long pRoleId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(Long pRoleId)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
