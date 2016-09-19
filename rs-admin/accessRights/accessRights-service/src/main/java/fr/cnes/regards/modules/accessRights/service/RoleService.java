/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Service
public class RoleService implements IRoleService {

    @Autowired
    private IRoleRepository roleRepository_;

    @Override
    public List<Role> retrieveRoleList() {
        return roleRepository_.findAll();
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        return roleRepository_.save(pNewRole);
    }

    @Override
    public Role retrieveRole(Long pRoleId) {
        return roleRepository_.findOne(pRoleId);
    }

    @Override
    public void updateRole(Long pRoleId, Role pUpdatedRole) throws OperationNotSupportedException {
        roleRepository_.save(pUpdatedRole);
    }

    @Override
    public void removeRole(Long pRoleId) {
        roleRepository_.delete(pRoleId);
    }

    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(Long pRoleId) {
        Role role = roleRepository_.findOne(pRoleId);
        return role.getPermissions();
    }

    @Override
    public void updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList) {
        Role role = roleRepository_.findOne(pRoleId);
        List<ResourcesAccess> permissions = role.getPermissions();

        // Finder method
        // Pass the id and the list to search, returns the element with passed id
        Function<Long, List<ResourcesAccess>> find = (id) -> {
            return pResourcesAccessList.stream().filter(e -> e.getId().equals(id)).collect(Collectors.toList());
        };
        Function<Long, Boolean> contains = (id) -> {
            return !find.apply(id).isEmpty();
        };
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        permissions.replaceAll(p -> contains.apply(p.getId()) ? find.apply(p.getId()).get(0) : p);

        role.setPermissions(permissions); // TODO Useless right?
        roleRepository_.save(role);
    }

    @Override
    public void clearRoleResourcesAccess(Long pRoleId) {
        Role role = roleRepository_.findOne(pRoleId);
        role.setPermissions(new ArrayList<>());
        roleRepository_.save(role);
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(Long pRoleId) {
        Role role = roleRepository_.findOne(pRoleId);
        return role.getProjectUsers();
    }

    @Override
    public boolean existRole(Long pRoleId) {
        return roleRepository_.exists(pRoleId);
    }

    @Override
    public boolean existRole(Role pRole) {
        return roleRepository_.exists(pRole.getId());
    }

    @Override
    public Role getDefaultRole() {
        return roleRepository_.findByIsDefault(true);
    }

}
