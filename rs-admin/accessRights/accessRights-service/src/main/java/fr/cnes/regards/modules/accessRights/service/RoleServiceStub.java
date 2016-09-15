/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.IDaoRole;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Service
public class RoleServiceStub implements IRoleService {

    private static List<Role> roles_;

    @Autowired
    private IDaoRole daoRole_;

    @Override
    @PostConstruct
    public void init() {
        roles_ = daoRole_.getAll();

    }

    @Override
    public List<Role> retrieveRoleList() {
        return roles_;
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        if (existRole(pNewRole.getRoleId())) {
            throw new AlreadyExistingException("" + pNewRole.getRoleId());
        }
        roles_.add(pNewRole);
        return pNewRole;
    }

    @Override
    public Role retrieveRole(Integer pRoleId) {
        return roles_.stream().filter(r -> r.getRoleId().equals(pRoleId)).findFirst().get();
    }

    @Override
    public void updateRole(Integer pRoleId, Role pUpdatedRole)
            throws NoSuchElementException, OperationNotSupportedException {
        if (!existRole(pRoleId)) {
            throw new NoSuchElementException("Role of given id (" + pRoleId + ") could not be found");
        }
        if (!pUpdatedRole.getRoleId().equals(pRoleId)) {
            throw new OperationNotSupportedException("Updated role does not match passed role id");
        }
        roles_.stream().map(r -> r.getRoleId().equals(pRoleId) ? pUpdatedRole : r).collect(Collectors.toList());
    }

    @Override
    public void removeRole(Integer pRoleId) {
        roles_ = roles_.stream().filter(r -> !r.getRoleId().equals(pRoleId)).collect(Collectors.toList());
    }

    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(Integer pRoleId) {
        Role role = retrieveRole(pRoleId);
        List<ResourcesAccess> resourcesAccesses = role.getPermissions();
        return resourcesAccesses;
    }

    @Override
    public void updateRoleResourcesAccess(Integer pRoleId, List<ResourcesAccess> pResourcesAccessList)
            throws NoSuchElementException {
        if (!existRole(pRoleId)) {
            throw new NoSuchElementException("Role of given id (" + pRoleId + ") could not be found");
        }
        Role role = retrieveRole(pRoleId);

        // Finder method
        // Pass the id and the list to search, returns the element with passed id
        Function<Integer, List<ResourcesAccess>> find = (id) -> {
            return pResourcesAccessList.stream().filter(e -> e.getResourcesAccessId().equals(id))
                    .collect(Collectors.toList());
        };
        Function<Integer, Boolean> contains = (id) -> {
            return !find.apply(id).isEmpty();
        };

        List<ResourcesAccess> permissions = role.getPermissions();
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        permissions.replaceAll(p -> contains.apply(p.getResourcesAccessId())
                ? find.apply(p.getResourcesAccessId()).get(0) : p);
    }

    @Override
    public void clearRoleResourcesAccess(Integer pRoleId) {
        Role role = retrieveRole(pRoleId);
        role.setPermissions(new ArrayList<>());
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(Integer pRoleId) {
        Role role = retrieveRole(pRoleId);
        List<ProjectUser> projectUsers = role.getProjectUsers();
        return projectUsers;
    }

    @Override
    public boolean existRole(Integer pRoleId) {
        return roles_.stream().filter(r -> r.getRoleId().equals(pRoleId)).findFirst().isPresent();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IRoleService#getDefaultRole()
     */
    @Override
    public Role getDefaultRole() {
        // TODO Auto-generated method stub
        return null;
    }

}
