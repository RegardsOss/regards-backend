/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import fr.cnes.regards.modules.accessRights.client.RolesClient;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

public class RolesFallback implements RolesClient {

    @Override
    public HttpEntity<List<Resource<Role>>> retrieveRoleList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Role>> createRole(Role pNewRole) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Role>> retrieveRole(Long pRoleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateRole(Long pRoleId, Role pUpdatedRole) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> removeRole(Long pRoleId) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(Long pRoleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> clearRoleResourcesAccess(Long pRoleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(Long pRoleId)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

}
