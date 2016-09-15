package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

/*
 * LICENSE_PLACEHOLDER
 */
public interface IRoleService {

    List<Role> retrieveRoleList();

    Role createRole(Role pNewRole) throws AlreadyExistingException;

    Role retrieveRole(Integer pRoleId);

    void updateRole(Integer pRoleId, Role pUpdatedRole) throws OperationNotSupportedException;

    void removeRole(Integer pRoleId);

    List<ResourcesAccess> retrieveRoleResourcesAccessList(Integer pRoleId);

    void updateRoleResourcesAccess(Integer pRoleId, List<ResourcesAccess> pResourcesAccessList);

    void clearRoleResourcesAccess(Integer pRoleId);

    List<ProjectUser> retrieveRoleProjectUserList(Integer pRoleId);

    boolean existRole(Integer pRoleId);

    void init();
}
