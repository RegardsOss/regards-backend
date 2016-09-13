package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

public interface IRoleService {

    List<Role> retrieveRoleList();

    Role createRole(Role pNewRole) throws AlreadyExistingException;

    Role retrieveRole(String pRoleId);

    void updateRole(String pRoleId, Role pUpdatedRole) throws OperationNotSupportedException;

    void removeRole(String pRoleId);

    List<ResourcesAccess> retrieveRoleResourcesAccessList(String pRoleId);

    void updateRoleResourcesAccess(String pRoleId, List<ResourcesAccess> pResourcesAccessList);

    void clearRoleResourcesAccess(String pRoleId);

    List<ProjectUser> retrieveRoleProjectUserList(String pRoleId);

}
