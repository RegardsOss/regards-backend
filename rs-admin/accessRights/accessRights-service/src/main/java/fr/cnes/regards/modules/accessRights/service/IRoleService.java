/*
 * LICENSE_PLACEHOLDER
 */
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

    Role retrieveRole(Long pRoleId);

    void updateRole(Long pRoleId, Role pUpdatedRole) throws OperationNotSupportedException;

    void removeRole(Long pRoleId);

    List<ResourcesAccess> retrieveRoleResourcesAccessList(Long pRoleId);

    void updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList);

    void clearRoleResourcesAccess(Long pRoleId);

    List<ProjectUser> retrieveRoleProjectUserList(Long pRoleId);

    boolean existRole(Long pRoleId);

    boolean existRole(Role pRole);

    Role getDefaultRole();


    boolean isHierarchicallyInferior(Role pRole, Role pOther);
}
