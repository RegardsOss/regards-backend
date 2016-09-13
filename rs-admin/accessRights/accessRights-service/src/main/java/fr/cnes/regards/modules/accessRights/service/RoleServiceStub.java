package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Service
public class RoleServiceStub implements IRoleService {

    private static List<Role> roles = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<ResourcesAccess> permissionList = new ArrayList<>();
        permissionList.add(new ResourcesAccess("ResourceAccess 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        permissionList.add(new ResourcesAccess("ResourceAccess 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        permissionList.add(new ResourcesAccess("ResourceAccess 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        permissionList.add(new ResourcesAccess("ResourceAccess 3", "Microservice 3", "Resource 3", HttpVerb.GET));

        List<ProjectUser> projectUsers = new ArrayList<>();
        projectUsers.add(new ProjectUser("laurel@cnes.fr"));
        projectUsers.add(new ProjectUser("hardy@cnes.fr"));

        roles.add(new Role(0, "Tête d'affiche", null, permissionList.subList(1, 2), projectUsers.subList(0, 1)));
        roles.add(new Role(1, "Second rôle", roles.get(0), permissionList.subList(0, 0), projectUsers.subList(0, 0)));
        roles.add(new Role(2, "Figurant", roles.get(0), permissionList.subList(0, 3), projectUsers.subList(1, 1)));
    }

    @Override
    public List<Role> retrieveRoleList() {
        return roles;
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        if (existRole(pNewRole.getName())) {
            throw new AlreadyExistingException(pNewRole.getName());
        }
        roles.add(pNewRole);
        return pNewRole;
    }

    @Override
    public Role retrieveRole(String pRoleId) {
        return roles.stream().filter(r -> r.getRoleId().equals(pRoleId)).findFirst().get();
    }

    @Override
    public void updateRole(String pRoleId, Role pUpdatedRole) throws OperationNotSupportedException {
        if (!existRole(pRoleId)) {
            throw new NoSuchElementException(pRoleId);
        }
        if (!pUpdatedRole.getRoleId().equals(pRoleId)) {
            throw new IllegalArgumentException("Updated role does not match passed role id");
        }
        roles.stream().map(r -> r.getRoleId().equals(pRoleId) ? pUpdatedRole : r).collect(Collectors.toList());
    }

    @Override
    public void removeRole(String pRoleId) {
        roles = roles.stream().filter(r -> r.getRoleId().equals(pRoleId)).collect(Collectors.toList());
    }

    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(String pRoleId) {
        Role role = retrieveRole(pRoleId);
        List<ResourcesAccess> resourcesAccesses = role.getPermissions();
        return resourcesAccesses;
    }

    @Override
    public void updateRoleResourcesAccess(String pRoleId, List<ResourcesAccess> pResourcesAccessList) {
        if (!existRole(pRoleId)) {
            throw new NoSuchElementException(pRoleId);
        }
        Role role = retrieveRole(pRoleId);
        role.setPermissions(pResourcesAccessList);
    }

    @Override
    public void clearRoleResourcesAccess(String pRoleId) {
        Role role = retrieveRole(pRoleId);
        role.setPermissions(new ArrayList<>());
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(String pRoleId) {
        Role role = retrieveRole(pRoleId);
        List<ProjectUser> projectUsers = role.getProjectUsers();
        return projectUsers;
    }

    public boolean existRole(String pRoleId) {
        return roles.stream().filter(r -> r.getRoleId().equals(pRoleId)).findFirst().isPresent();
    }

}
