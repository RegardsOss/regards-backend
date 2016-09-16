/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.IDaoRole;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Service
public class RoleService implements IRoleService {

    @Autowired
    private IDaoRole daoRole_;

    @Override
    public List<Role> retrieveRoleList() {
        return daoRole_.retrieveRoleList();
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        return daoRole_.createRole(pNewRole);
    }

    @Override
    public Role retrieveRole(Integer pRoleId) {
        return daoRole_.retrieveRole(pRoleId);
    }

    @Override
    public void updateRole(Integer pRoleId, Role pUpdatedRole) throws OperationNotSupportedException {
        daoRole_.updateRole(pRoleId, pUpdatedRole);
    }

    @Override
    public void removeRole(Integer pRoleId) {
        daoRole_.removeRole(pRoleId);
    }

    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(Integer pRoleId) {
        return daoRole_.retrieveRoleResourcesAccessList(pRoleId);
    }

    @Override
    public void updateRoleResourcesAccess(Integer pRoleId, List<ResourcesAccess> pResourcesAccessList) {
        daoRole_.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
    }

    @Override
    public void clearRoleResourcesAccess(Integer pRoleId) {
        daoRole_.clearRoleResourcesAccess(pRoleId);
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(Integer pRoleId) {
        return daoRole_.retrieveRoleProjectUserList(pRoleId);
    }

    @Override
    public boolean existRole(Integer pRoleId) {
        return daoRole_.existRole(pRoleId);
    }

    @Override
    public Role getDefaultRole() {
        return daoRole_.getDefaultRole();
    }

    @Override
    public boolean existRole(Role pRole) {
        return daoRole_.existRole(pRole);
    }

}
