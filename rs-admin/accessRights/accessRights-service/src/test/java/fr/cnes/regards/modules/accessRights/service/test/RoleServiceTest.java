package fr.cnes.regards.modules.accessRights.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@RunWith(SpringJUnit4ClassRunner.class)
public class RoleServiceTest {

    @Autowired
    private IRoleService roleService_;

    @Test
    public void retrieveRoleList() {
        List<Role> roles = roleService_.retrieveRoleList();

        assertTrue(!roles.isEmpty());
    }

    @Test(expected = AlreadyExistingException.class)
    public void createRoleDuplicate() throws AlreadyExistingException {
        Integer roleId = 0;
        assertTrue(roleService_.existRole(roleId));
        Role duplicate = new Role(0);

        roleService_.createRole(duplicate);
    }

    @Test
    public void createRole() throws AlreadyExistingException {
        Integer roleId = 4834848;
        Role expected = new Role(roleId);
        assertTrue(!roleService_.existRole(roleId));

        Role actual = roleService_.createRole(expected);

        assertEquals(expected, actual);
    }

    @Test(expected = NoSuchElementException.class)
    public void updateRoleNotExistent() throws NoSuchElementException, OperationNotSupportedException {
        Integer roleId = 58354;
        assertTrue(!roleService_.existRole(roleId));
        Role notExistent = new Role(roleId);

        roleService_.updateRole(roleId, notExistent);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void updateRoleWrongId() throws NoSuchElementException, OperationNotSupportedException {
        Integer roleIdPassed = 58354;
        Integer idOfPassedRole = 9999;
        assertTrue(!roleIdPassed.equals(idOfPassedRole));
        Role passedRole = new Role(idOfPassedRole);

        roleService_.updateRole(roleIdPassed, passedRole);
    }

    @Test
    public void updateRole() throws NoSuchElementException, OperationNotSupportedException {
        Integer passedRoleId = 0;
        assertTrue(roleService_.existRole(passedRoleId));
        Role previousRole = roleService_.retrieveRole(passedRoleId);
        Role passedRole = new Role(passedRoleId, "new name", null, null, null, true, false);

        // Ensure new role attributes are different from the previous
        assertTrue(!roleService_.retrieveRole(passedRoleId).getName().equals(previousRole.getName()));
        assertTrue(!roleService_.retrieveRole(passedRoleId).getParentRole().equals(previousRole.getParentRole()));
        assertTrue(!roleService_.retrieveRole(passedRoleId).getPermissions().equals(previousRole.getPermissions()));
        assertTrue(!roleService_.retrieveRole(passedRoleId).getProjectUsers().equals(previousRole.getProjectUsers()));

        roleService_.updateRole(passedRoleId, passedRole);
        Role updatedRole = roleService_.retrieveRole(passedRoleId);

        // Ensure they are now equal
        assertTrue(updatedRole.getRoleId().equals(passedRole.getRoleId()));
        assertTrue(updatedRole.getName().equals(passedRole.getName()));
        assertTrue(updatedRole.getParentRole().equals(passedRole.getParentRole()));
        assertTrue(updatedRole.getPermissions().equals(passedRole.getPermissions()));
        assertTrue(updatedRole.getProjectUsers().equals(passedRole.getProjectUsers()));
    }

    @Test
    public void removeRole() {
        Integer roleId = 0;
        assertTrue(roleService_.existRole(roleId));

        roleService_.removeRole(roleId);

        assertTrue(!roleService_.existRole(roleId));
    }

    @Test(expected = NoSuchElementException.class)
    public void updateRoleResourcesAccessNotExistent() throws NoSuchElementException {
        Integer roleId = 44255;
        assertTrue(!roleService_.existRole(roleId));
        List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        roleService_.updateRoleResourcesAccess(roleId, resourcesAccesses);
    }

    @Test
    public void updateRoleResourcesAccessAddingResourcesAccess() throws NoSuchElementException {
        Integer roleId = 0;
        assertTrue(roleService_.existRole(roleId));
        List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645, "", "", "", HttpVerb.PATCH);
        resourcesAccesses.add(addedResourcesAccess);

        roleService_.updateRoleResourcesAccess(roleId, resourcesAccesses);

        assertTrue(roleService_.retrieveRole(roleId).getPermissions().contains(addedResourcesAccess));
    }

    @Test
    public void updateRoleResourcesAccessUpdatingResourcesAccess() throws NoSuchElementException {
        Integer roleId = 0;
        assertTrue(roleService_.existRole(roleId));
        Role role = roleService_.retrieveRole(roleId);

        ResourcesAccess previousResourcesAccess = role.getPermissions().get(0);
        ResourcesAccess passedResourcesAccess = new ResourcesAccess(roleId, "new desc", "new mic", "new res",
                HttpVerb.TRACE);

        // Ensure new permission's attributes are different from the previous
        assertTrue(!passedResourcesAccess.getDescription().equals(previousResourcesAccess.getDescription()));
        assertTrue(!passedResourcesAccess.getMicroservice().equals(previousResourcesAccess.getMicroservice()));
        assertTrue(!passedResourcesAccess.getResource().equals(previousResourcesAccess.getResource()));
        assertTrue(!passedResourcesAccess.getVerb().equals(previousResourcesAccess.getVerb()));

        List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        resourcesAccesses.add(passedResourcesAccess);
        roleService_.updateRoleResourcesAccess(roleId, resourcesAccesses);

        // Ensure they are now equal
        ResourcesAccess updatedResourcesAccess = role.getPermissions().get(0);
        assertTrue(updatedResourcesAccess.getDescription().equals(updatedResourcesAccess.getDescription()));
        assertTrue(updatedResourcesAccess.getMicroservice().equals(updatedResourcesAccess.getMicroservice()));
        assertTrue(updatedResourcesAccess.getResource().equals(updatedResourcesAccess.getResource()));
        assertTrue(updatedResourcesAccess.getVerb().equals(updatedResourcesAccess.getVerb()));
    }

    @Test
    public void clearRoleResourcesAccess() {
        Integer roleId = 0;
        Role role = roleService_.retrieveRole(roleId);
        assertTrue(!role.getPermissions().isEmpty());

        roleService_.clearRoleResourcesAccess(roleId);

        assertTrue(!role.getPermissions().isEmpty());
    }

    @Test
    public void retrieveRoleProjectUserList() {
        Integer roleId = 0;
        Role role = roleService_.retrieveRole(roleId);
        List<ProjectUser> expected = role.getProjectUsers();
        assertNotNull(expected);

        List<ProjectUser> actual = roleService_.retrieveRoleProjectUserList(roleId);

        assertEquals(expected, actual);
    }

}
