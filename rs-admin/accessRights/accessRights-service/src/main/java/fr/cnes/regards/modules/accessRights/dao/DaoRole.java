/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Repository
public class DaoRole implements IDaoRole {

    @Override
    public List<Role> retrieveRoleList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Role retrieveRole(Integer pRoleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateRole(Integer pRoleId, Role pUpdatedRole) throws OperationNotSupportedException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeRole(Integer pRoleId) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(Integer pRoleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateRoleResourcesAccess(Integer pRoleId, List<ResourcesAccess> pResourcesAccessList) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearRoleResourcesAccess(Integer pRoleId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(Integer pRoleId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean existRole(Integer pRoleId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Role getDefaultRole() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean existRole(Role pRole) {
        // TODO Auto-generated method stub
        return false;
    }

}
