/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessRights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessRights.dao.stubs.RoleRepositoryStub;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.service.RoleService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

public class RoleServiceTest {

    private IRoleService roleService_;

    @Before
    public void init() throws AlreadyExistingException {
        IRoleRepository roleRepository = new RoleRepositoryStub();
        roleService_ = new RoleService(roleRepository);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        List<Role> roles = roleService_.retrieveRoleList();

        assertTrue(!roles.isEmpty());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role.")
    public void retrieveRole() {
        Role role = roleService_.retrieveRole(0L);
        assertNotNull(role);
    }

    @Test(expected = AlreadyExistingException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to create an already existing role.")
    public void createRoleDuplicate() throws AlreadyExistingException {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));
        Role duplicate = new Role(0L);

        roleService_.createRole(duplicate);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role in a regular case.")
    public void createRole() throws AlreadyExistingException {
        Long roleId = 4834848L;
        Role expected = new Role(roleId);
        assertTrue(!roleService_.existRole(roleId));

        Role actual = roleService_.createRole(expected);

        assertEquals(expected, actual);
    }

    @Test(expected = NoSuchElementException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which does not exist.")
    public void updateRoleNotExistent() throws NoSuchElementException, OperationNotSupportedException {
        Long roleId = 58354L;
        assertTrue(!roleService_.existRole(roleId));
        Role notExistent = new Role(roleId);

        roleService_.updateRole(roleId, notExistent);
    }

    @Test(expected = OperationNotSupportedException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which id is different from the passed one.")
    public void updateRoleWrongId() throws NoSuchElementException, OperationNotSupportedException {
        Long roleId = 58354L;
        Role role = new Role(9999L);
        assertTrue(!roleId.equals(role.getId()));

        roleService_.updateRole(roleId, role);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role in a regular case.")
    public void updateRole() throws NoSuchElementException, OperationNotSupportedException {
        Long passedRoleId = 0L;
        assertTrue(roleService_.existRole(passedRoleId));
        Role previousRole = roleService_.retrieveRole(passedRoleId);
        Role passedRole = new Role(passedRoleId, "new name", null, new ArrayList<>(), new ArrayList<>());

        // Ensure at least one is different from the previous
        assertTrue(passedRole.getName() != previousRole.getName());

        roleService_.updateRole(passedRoleId, passedRole);

        // Ensure they are now equal
        Role updatedRole = roleService_.retrieveRole(passedRoleId);
        assertTrue(updatedRole.equals(passedRole));
        assertTrue(updatedRole.getId().equals(passedRole.getId()));
        assertTrue(updatedRole.getName().equals(passedRole.getName()));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role in a regular case.")
    public void removeRole() {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));

        roleService_.removeRole(roleId);

        assertTrue(!roleService_.existRole(roleId));
    }

    @Test(expected = NoSuchElementException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update permissions of a role which does not exist.")
    public void updateRoleResourcesAccessNotExistent() throws NoSuchElementException {
        Long roleId = 44255L;
        assertTrue(!roleService_.existRole(roleId));
        List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        roleService_.updateRoleResourcesAccess(roleId, resourcesAccesses);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to add resources accesses on a role.")
    public void updateRoleResourcesAccessAddingResourcesAccess() throws NoSuchElementException {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));
        List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645L, "", "", "", HttpVerb.PATCH);
        resourcesAccesses.add(addedResourcesAccess);

        roleService_.updateRoleResourcesAccess(roleId, resourcesAccesses);

        assertTrue(roleService_.retrieveRole(roleId).getPermissions().contains(addedResourcesAccess));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccessUpdatingResourcesAccess() throws NoSuchElementException {
        Long roleId = 0L;
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
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        Long roleId = 0L;
        Role role = roleService_.retrieveRole(roleId);
        assertTrue(!role.getPermissions().isEmpty());

        roleService_.clearRoleResourcesAccess(roleId);

        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        Long roleId = 0L;
        Role role = roleService_.retrieveRole(roleId);
        List<ProjectUser> expected = role.getProjectUsers();
        assertNotNull(expected);

        List<ProjectUser> actual = roleService_.retrieveRoleProjectUserList(roleId);

        assertEquals(expected, actual);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        Long roleId = 0L;

        assertTrue(!roleService_.retrieveRoleResourcesAccessList(roleId).isEmpty());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system is able to hierarchically compare two roles.")
    public void isHierarchicallyInferior() {
        Role registeredUser = roleService_.retrieveRole(1L);
        Role projectAdmin = roleService_.retrieveRole(3L);

        assertTrue(roleService_.isHierarchicallyInferior(registeredUser, projectAdmin));
        assertFalse(roleService_.isHierarchicallyInferior(projectAdmin, registeredUser));

        Role admin = roleService_.retrieveRole(2L);
        Role customRoleFromAdmin = new Role(99L, "custom role", admin, new ArrayList<>(), new ArrayList<>());

        assertFalse(roleService_.isHierarchicallyInferior(customRoleFromAdmin, registeredUser));
        assertFalse(roleService_.isHierarchicallyInferior(customRoleFromAdmin, projectAdmin));
    }

}
