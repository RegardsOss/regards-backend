
package fr.cnes.regards.modules.accessRights.dao.test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IDaoProjectUser;
import fr.cnes.regards.modules.accessRights.dao.IDaoResourcesAccess;
import fr.cnes.regards.modules.accessRights.dao.IDaoRole;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Repository
public class DaoRoleStub implements IDaoRole {

    private List<Role> roles_;

    @Autowired
    private IDaoResourcesAccess daoResourcesAccess_;

    @Autowired
    private IDaoProjectUser daoProjectUser_;

    @PostConstruct
    public void init() {
        List<ResourcesAccess> permissionList_ = daoResourcesAccess_.getAll();
        List<ProjectUser> projectUsers_ = daoProjectUser_.getAll();

        // Init default roles
        Role rolePublic = new Role(0, "Public", null, null, projectUsers_.subList(8, 10), true, true);
        Role roleRegisteredUser = new Role(1, "Registered User", rolePublic, null, projectUsers_.subList(5, 8), false,
                true);
        Role roleAdmin = new Role(2, "Admin", roleRegisteredUser, null, projectUsers_.subList(3, 5), false, true);
        Role roleProjectAdmin = new Role(3, "Project Admin", roleAdmin, null, projectUsers_.subList(1, 3), false, true);
        Role roleInstanceAdmin = new Role(4, "Instance Admin", roleProjectAdmin, permissionList_,
                projectUsers_.subList(0, 1), false, true);
        roles_.add(rolePublic);
        roles_.add(roleRegisteredUser);
        roles_.add(roleAdmin);
        roles_.add(roleProjectAdmin);
        roles_.add(roleInstanceAdmin);

        // Init some custom roles
        Role role0 = new Role(5, "Role 0", rolePublic, permissionList_.subList(1, 2), projectUsers_.subList(0, 1));
        Role role1 = new Role(6, "Role 1", rolePublic, permissionList_.subList(0, 2), projectUsers_.subList(1, 2));
        Role role2 = new Role(7, "Role 2", rolePublic, permissionList_.subList(1, 3), projectUsers_.subList(0, 2));
        roles_.add(role0);
        roles_.add(role1);
        roles_.add(role2);
    }

    @Override
    public List<Role> retrieveRoleList() {
        return roles_;
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        if (existRole(pNewRole)) {
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

    @Override
    public boolean existRole(Role role) {
        return roles_.contains(role);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IRoleService#getDefaultRole()
     */
    @Override
    public Role getDefaultRole() {
        return retrieveRoleList().stream().filter(r -> r.isDefault()).findFirst().get();
    }
}
