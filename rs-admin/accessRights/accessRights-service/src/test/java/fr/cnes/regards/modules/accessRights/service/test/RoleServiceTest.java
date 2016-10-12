/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessRights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.service.RoleService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

/**
 * Test class for {@link RoleService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class RoleServiceTest {

    /**
     * The tested service
     */
    private IRoleService roleService;

    /**
     * Mock repository
     */
    private IRoleRepository roleRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() throws AlreadyExistingException {
        // IRoleRepository roleRepository = new RoleRepositoryStub();
        roleRepository = Mockito.mock(IRoleRepository.class);
        roleService = new RoleService(roleRepository);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        final List<Role> expected = new ArrayList<>();
        final Role role0 = new Role(0L, "name", null, new ArrayList<>(), new ArrayList<>());
        final Role role1 = new Role(1L, "otherName", role0, new ArrayList<>(), new ArrayList<>());
        expected.add(role0);
        expected.add(role1);

        Mockito.when(roleRepository.findAll()).thenReturn(expected);
        final List<Role> actual = roleService.retrieveRoleList();

        // Check that the expected and actual role have same values
        checkRolesEqual(expected.get(0), actual.get(0));
        checkRolesEqual(expected.get(1), actual.get(1));
        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findAll();
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role.")
    public void retrieveRole() {
        final Long id = 0L;
        final Role expected = new Role(id, "name", null, new ArrayList<>(), new ArrayList<>());

        Mockito.when(roleRepository.findOne(id)).thenReturn(expected);
        final Role actual = roleService.retrieveRole(id);

        // Check that the expected and actual role have same values
        checkRolesEqual(expected, actual);
        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(id);
    }

    @Test(expected = AlreadyExistingException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to create an already existing role.")
    public void createRoleDuplicate() throws AlreadyExistingException {
        final Long id = 0L;
        Mockito.when(roleRepository.exists(id)).thenReturn(true);

        final Role duplicate = new Role(id);
        roleService.createRole(duplicate);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role in a regular case.")
    public void createRole() throws AlreadyExistingException {
        final Long id = 4834848L;
        final Role expected = new Role(id);
        Mockito.when(roleRepository.save(expected)).thenReturn(expected);

        final Role actual = roleService.createRole(expected);
        Mockito.when(roleRepository.findOne(id)).thenReturn(actual);

        // Check that the expected and actual role have same values
        checkRolesEqual(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(expected);
    }

    @Test(expected = NoSuchElementException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which does not exist.")
    public void updateRoleNotExistent() throws NoSuchElementException, OperationNotSupportedException {
        final Long id = 58354L;
        final Role notExistent = new Role(id);
        Mockito.when(roleRepository.exists(id)).thenReturn(false);

        roleService.updateRole(id, notExistent);
    }

    @Test(expected = OperationNotSupportedException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which id is different from the passed one.")
    public void updateRoleWrongId() throws NoSuchElementException, OperationNotSupportedException {
        final Long id = 58354L;
        final Role role = new Role(9999L);
        assertTrue(!id.equals(role.getId()));

        roleService.updateRole(id, role);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role in a regular case.")
    public void updateRole() throws NoSuchElementException, OperationNotSupportedException {
        final Long passedRoleId = 0L;

        // Define the previous role in db
        final Role previousRole = new Role(passedRoleId, "name", null, new ArrayList<>(), new ArrayList<>());
        Mockito.when(roleRepository.exists(passedRoleId)).thenReturn(true);

        // Define the passed role in PUT
        final Role passedRole = new Role(passedRoleId, "new name", null, new ArrayList<>(), new ArrayList<>());

        // Ensure at least one is different from the previous
        assertTrue(passedRole.getName() != previousRole.getName());

        // Do the update
        roleService.updateRole(passedRoleId, passedRole);

        // Retrieve the updated role
        Mockito.when(roleRepository.findOne(passedRoleId)).thenReturn(passedRole);
        final Role updatedRole = roleService.retrieveRole(passedRoleId);

        // Ensure they are now equal
        checkRolesEqual(passedRole, updatedRole);

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(passedRole);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role in a regular case.")
    public void removeRole() {
        final Long id = 0L;

        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Assert.assertTrue(roleService.existRole(id));

        roleService.removeRole(id);

        // Check it was removed
        Mockito.when(roleRepository.exists(id)).thenReturn(false);
        Assert.assertFalse(roleService.existRole(id));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).delete(id);
    }

    @Test(expected = NoSuchElementException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update permissions of a role which does not exist.")
    public void updateRoleResourcesAccessNotExistent() throws NoSuchElementException {
        final Long id = 44255L;

        Mockito.when(roleRepository.exists(id)).thenReturn(false);
        assertTrue(!roleService.existRole(id));

        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        roleService.updateRoleResourcesAccess(id, resourcesAccesses);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to add resources accesses on a role.")
    public void updateRoleResourcesAccessAddingResourcesAccess() throws NoSuchElementException {
        final Long id = 0L;
        final Role role = new Role(id, "name", null, new ArrayList<>(), new ArrayList<>());

        // Mock
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(role);
        assertTrue(roleService.existRole(id));

        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        final ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645L, "", "", "", HttpVerb.PATCH);
        resourcesAccesses.add(addedResourcesAccess);

        roleService.updateRoleResourcesAccess(id, resourcesAccesses);

        // Prepare the result
        final Role expected = new Role(id, "name", null, resourcesAccesses, new ArrayList<>());
        Mockito.when(roleRepository.findOne(id)).thenReturn(expected);

        // Check
        Assert.assertTrue(roleService.retrieveRole(id).getPermissions().contains(addedResourcesAccess));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(expected);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccessUpdatingResourcesAccess() throws NoSuchElementException {
        final Long roleId = 0L;
        final List<ResourcesAccess> initRAs = new ArrayList<>();
        initRAs.add(new ResourcesAccess(0L, "desc", "mic", "res", HttpVerb.TRACE));
        final Role role = new Role(0L, "name", null, initRAs, new ArrayList<>());

        Mockito.when(roleRepository.exists(roleId)).thenReturn(true);
        Mockito.when(roleRepository.findOne(roleId)).thenReturn(role);
        assertTrue(roleService.existRole(roleId));

        final List<ResourcesAccess> passedRAs = new ArrayList<>();
        passedRAs.add(new ResourcesAccess(0L, "new desc", "new mic", "new res", HttpVerb.DELETE));

        // Ensure new permission's attributes are different from the previous
        assertTrue(!passedRAs.get(0).getDescription().equals(initRAs.get(0).getDescription()));
        assertTrue(!passedRAs.get(0).getMicroservice().equals(initRAs.get(0).getMicroservice()));
        assertTrue(!passedRAs.get(0).getResource().equals(initRAs.get(0).getResource()));
        assertTrue(!passedRAs.get(0).getVerb().equals(initRAs.get(0).getVerb()));

        roleService.updateRoleResourcesAccess(roleId, passedRAs);

        // Ensure they are now equal
        final Role updatedRole = roleService.retrieveRole(roleId);
        final List<ResourcesAccess> updatedRAs = updatedRole.getPermissions();
        assertTrue(updatedRAs.get(0).getDescription().equals(passedRAs.get(0).getDescription()));
        assertTrue(updatedRAs.get(0).getMicroservice().equals(passedRAs.get(0).getMicroservice()));
        assertTrue(updatedRAs.get(0).getResource().equals(passedRAs.get(0).getResource()));
        assertTrue(updatedRAs.get(0).getVerb().equals(passedRAs.get(0).getVerb()));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        final Long id = 0L;
        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        resourcesAccesses.add(new ResourcesAccess(0L, "desc", "mic", "res", HttpVerb.TRACE));
        final Role role = new Role(id, "name", null, resourcesAccesses, new ArrayList<>());
        assertTrue(!role.getPermissions().isEmpty());

        // Mock
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(role);

        roleService.clearRoleResourcesAccess(id);

        // Retrieve updated role
        final Role updated = roleService.retrieveRole(id);

        // Check
        Assert.assertTrue(updated.getPermissions().isEmpty());

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(role);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        final Long id = 0L;
        final List<ProjectUser> expected = new ArrayList<>();
        expected.add(new ProjectUser());
        final Role role = new Role(0L, "name", null, null, expected);
        Assert.assertNotNull(expected);

        // Mock
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(role);

        final List<ProjectUser> actual = roleService.retrieveRoleProjectUserList(id);

        // Check
        Assert.assertThat(actual.get(0).getId(), CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getId())));
        Assert.assertThat(actual.get(0).getEmail(), CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getEmail())));
        Assert.assertThat(actual.get(0).getLastConnection(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getLastConnection())));
        Assert.assertThat(actual.get(0).getLastUpdate(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getLastUpdate())));
        Assert.assertThat(actual.get(0).getMetaData(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getMetaData())));
        Assert.assertThat(actual.get(0).getPermissions(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getPermissions())));
        Assert.assertThat(actual.get(0).getRole(), CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getRole())));
        Assert.assertThat(actual.get(0).getStatus(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getStatus())));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(id);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        final Long id = 0L;
        final List<ResourcesAccess> expected = new ArrayList<>();
        expected.add(new ResourcesAccess(0L, "desc", "mic", "res", HttpVerb.TRACE));
        final Role role = new Role(id, "name", null, expected, new ArrayList<>());
        assertTrue(!role.getPermissions().isEmpty());

        // Mock
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(role);

        final List<ResourcesAccess> actual = roleService.retrieveRoleResourcesAccessList(id);

        // Check
        Assert.assertThat(actual.get(0).getId(), CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getId())));
        Assert.assertThat(actual.get(0).getDescription(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getDescription())));
        Assert.assertThat(actual.get(0).getMicroservice(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getMicroservice())));
        Assert.assertThat(actual.get(0).getResource(),
                          CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getResource())));
        Assert.assertThat(actual.get(0).getVerb(), CoreMatchers.is(CoreMatchers.equalTo(expected.get(0).getVerb())));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(id);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system is able to hierarchically compare two roles.")
    public void isHierarchicallyInferior() {
        // Init default roles
        final Role rolePublic = new Role(0L, "Plublic", null, null, null);
        final Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, null);
        final Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, null, null);
        final Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, null, null);
        final Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, null, null);

        Assert.assertTrue(roleService.isHierarchicallyInferior(roleRegisteredUser, roleProjectAdmin));
        Assert.assertFalse(roleService.isHierarchicallyInferior(roleProjectAdmin, roleRegisteredUser));

        final Role admin = roleService.retrieveRole(2L);
        final Role customRoleFromAdmin = new Role(99L, "custom role", admin, null, null);

        Assert.assertFalse(roleService.isHierarchicallyInferior(customRoleFromAdmin, roleRegisteredUser));
        Assert.assertFalse(roleService.isHierarchicallyInferior(customRoleFromAdmin, roleProjectAdmin));
    }

    /**
     * Check that the passed {@link Role} has same attributes as the passed {@link Role}.
     *
     * @param pExpected
     *            The expected role
     * @param pActual
     *            The actual role
     */
    private void checkRolesEqual(final Role pExpected, final Role pActual) {
        Assert.assertThat(pActual.getId(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getId())));
        Assert.assertThat(pActual.getName(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getName())));
        Assert.assertThat(pActual.getParentRole(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getParentRole())));
        Assert.assertThat(pActual.getPermissions(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getPermissions())));
        Assert.assertThat(pActual.getProjectUsers(),
                          CoreMatchers.is(CoreMatchers.equalTo(pExpected.getProjectUsers())));
    }

}
