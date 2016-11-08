/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.test;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.IRoleService;
import fr.cnes.regards.modules.accessrights.service.RoleService;

/**
 * Test class for {@link RoleService}.
 *
 * @author xbrochar
 */
public class RoleServiceTest {

    /**
     * A name
     */
    private static final String NAME = "name";

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
    public void init() {
        roleRepository = Mockito.mock(IRoleRepository.class);
        roleService = new RoleService(roleRepository);
    }

    /**
     * Check that the allows to retrieve roles.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        final List<Role> expected = new ArrayList<>();
        final Role role0 = new Role(0L, NAME, null, new ArrayList<>(), new ArrayList<>());
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

    /**
     * Check that the allows to retrieve a single role.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role.")
    public void retrieveRole() {
        final Long id = 0L;
        final Role expected = new Role(id, NAME, null, new ArrayList<>(), new ArrayList<>());

        Mockito.when(roleRepository.findOne(id)).thenReturn(expected);
        final Role actual = roleService.retrieveRole(id);

        // Check that the expected and actual role have same values
        checkRolesEqual(expected, actual);
        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(id);
    }

    /**
     * Check that the system fails when trying to create an already existing role.
     *
     * @throws AlreadyExistingException
     *             Thrown if a role with passed id already exists
     */
    @Test(expected = AlreadyExistingException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to create an already existing role.")
    public void createRoleDuplicate() throws AlreadyExistingException {
        final Long id = 0L;
        Mockito.when(roleRepository.exists(id)).thenReturn(true);

        final Role duplicate = new Role(id);
        roleService.createRole(duplicate);
    }

    /**
     * Check that the system allows to create a role in a regular case.
     *
     * @throws AlreadyExistingException
     *             Thrown if a role with passed id already exists
     */
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

    /**
     * Check that the system fails when trying to update a role which does not exist.
     *
     * @throws EntityNotFoundException
     *             when no {@link Role} with passed <code>id</code> could be found<br/>
     * @throws InvalidValueException
     *             Thrown if passed role id differs from the id of the passed role
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which does not exist.")
    public void updateRoleNotExistent() throws EntityNotFoundException, InvalidValueException {
        final Long id = 58354L;
        final Role notExistent = new Role(id);
        Mockito.when(roleRepository.exists(id)).thenReturn(false);

        roleService.updateRole(id, notExistent);
    }

    /**
     * Check that the system fails when trying to update a role which id is different from the passed one.
     *
     * @throws EntityNotFoundException
     *             when no {@link Role} with passed <code>id</code> could be found<br/>
     * @throws InvalidValueException
     *             Thrown if passed role id differs from the id of the passed role
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which id is different from the passed one.")
    public void updateRoleWrongId() throws EntityNotFoundException, InvalidValueException {
        final Long id = 58354L;
        final Role role = new Role(9999L);
        Assert.assertTrue(!id.equals(role.getId()));

        roleService.updateRole(id, role);
    }

    /**
     * Check that the system allows to update a role in a regular case.
     *
     * @throws EntityNotFoundException
     *             when no {@link Role} with passed <code>id</code> could be found<br/>
     * @throws InvalidValueException
     *             Thrown if passed role id differs from the id of the passed role
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role in a regular case.")
    public void updateRole() throws EntityNotFoundException, InvalidValueException {
        final Long passedRoleId = 0L;

        // Define the previous role in db
        final Role previousRole = new Role(passedRoleId, NAME, null, new ArrayList<>(), new ArrayList<>());
        Mockito.when(roleRepository.exists(passedRoleId)).thenReturn(true);

        // Define the passed role in PUT
        final Role passedRole = new Role(passedRoleId, "new name", null, new ArrayList<>(), new ArrayList<>());

        // Ensure at least one is different from the previous
        Assert.assertTrue(passedRole.getName() != previousRole.getName());

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

    /**
     * Check that the system does not remove a native role.
     *
     * @throws OperationForbiddenException
     *             when the updated role is native. Native roles should not be modified.
     */
    @Test(expected = OperationForbiddenException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system does not remove a native role.")
    public void removeRoleNative() throws OperationForbiddenException {
        final Long id = 0L;
        final Role roleNative = new Role(id, NAME, null, new ArrayList<>(), new ArrayList<>(), true, true);

        // Mock repo
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(roleNative);

        // Call tested method
        roleService.removeRole(id);
    }

    /**
     * Check that the system allows to delete a role in a regular case.
     *
     * @throws OperationForbiddenException
     *             when the updated role is native. Native roles should not be modified.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role in a regular case.")
    public void removeRole() throws OperationForbiddenException {
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

    /**
     * Check that the system fails when trying to update permissions of a role which does not exist.
     *
     * @throws EntityNotFoundException
     *             Thrown if no role with passed id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update permissions of a role which does not exist.")
    public void updateRoleResourcesAccessNotExistent() throws EntityNotFoundException {
        final Long id = 44255L;

        Mockito.when(roleRepository.exists(id)).thenReturn(false);
        Assert.assertTrue(!roleService.existRole(id));

        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        roleService.updateRoleResourcesAccess(id, resourcesAccesses);
    }

    /**
     * Check that the system allows to add resources accesses on a role.
     *
     * @throws EntityNotFoundException
     *             Thrown if no role with passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to add resources accesses on a role.")
    public void updateRoleResourcesAccessAddingResourcesAccess() throws EntityNotFoundException {
        final Long id = 0L;
        final Role role = new Role(id, NAME, null, new ArrayList<>(), new ArrayList<>());

        // Mock
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(role);
        Assert.assertTrue(roleService.existRole(id));

        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        final ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645L, "", "", "", HttpVerb.PATCH);
        resourcesAccesses.add(addedResourcesAccess);

        // Perform the update
        roleService.updateRoleResourcesAccess(id, resourcesAccesses);

        // Prepare the expected result
        final Role expected = new Role(id, NAME, null, resourcesAccesses, new ArrayList<>());
        Mockito.when(roleRepository.findOne(id)).thenReturn(expected);

        // Check
        Assert.assertTrue(roleService.retrieveRole(id).getPermissions().contains(addedResourcesAccess));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update resources accesses of a role.
     *
     * @throws EntityNotFoundException
     *             Thrown if no role with passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccessUpdatingResourcesAccess() throws EntityNotFoundException {
        final Long roleId = 0L;
        final List<ResourcesAccess> initRAs = new ArrayList<>();
        initRAs.add(new ResourcesAccess(0L, "desc", "mic", "res", HttpVerb.TRACE));
        final Role role = new Role(0L, NAME, null, initRAs, new ArrayList<>());

        Mockito.when(roleRepository.exists(roleId)).thenReturn(true);
        Mockito.when(roleRepository.findOne(roleId)).thenReturn(role);
        Assert.assertTrue(roleService.existRole(roleId));

        final List<ResourcesAccess> passedRAs = new ArrayList<>();
        passedRAs.add(new ResourcesAccess(0L, "new desc", "new mic", "new res", HttpVerb.DELETE));

        // Ensure new permission's attributes are different from the previous
        Assert.assertTrue(!passedRAs.get(0).getDescription().equals(initRAs.get(0).getDescription()));
        Assert.assertTrue(!passedRAs.get(0).getMicroservice().equals(initRAs.get(0).getMicroservice()));
        Assert.assertTrue(!passedRAs.get(0).getResource().equals(initRAs.get(0).getResource()));
        Assert.assertTrue(!passedRAs.get(0).getVerb().equals(initRAs.get(0).getVerb()));

        // Perform the update
        Role updatedRole = new Role(0L, NAME, null, passedRAs, new ArrayList<>());
        Mockito.when(roleRepository.save(updatedRole)).thenReturn(updatedRole);
        updatedRole = roleService.updateRoleResourcesAccess(roleId, passedRAs);

        // Ensure they are now equal
        final List<ResourcesAccess> updatedRAs = updatedRole.getPermissions();
        Assert.assertTrue(updatedRAs.get(0).getDescription().equals(passedRAs.get(0).getDescription()));
        Assert.assertTrue(updatedRAs.get(0).getMicroservice().equals(passedRAs.get(0).getMicroservice()));
        Assert.assertTrue(updatedRAs.get(0).getResource().equals(passedRAs.get(0).getResource()));
        Assert.assertTrue(updatedRAs.get(0).getVerb().equals(passedRAs.get(0).getVerb()));
    }

    /**
     * Check that the system allows to remove all resources accesses of a role.
     *
     * @throws EntityNotFoundException
     *             Thrown if no role with passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() throws EntityNotFoundException {
        final Long id = 0L;
        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        resourcesAccesses.add(new ResourcesAccess(0L, "desc", "mic", "res", HttpVerb.TRACE));
        final Role role = new Role(id, NAME, null, resourcesAccesses, new ArrayList<>());
        Assert.assertTrue(!role.getPermissions().isEmpty());

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

    /**
     * Check that the system allows to retrieve all users from a role hierarchy.
     *
     * @throws EntityNotFoundException
     *             Thrown when no entity of passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users  from a role hierarchy.")
    public void retrieveRoleProjectUserList() throws EntityNotFoundException {
        final Long idParent = 0L;
        final Long idChild = 1L;

        // Define a parent role with a few users
        final List<ProjectUser> parentUsers = new ArrayList<>();
        final Role roleParent = new Role(idParent, "parent", null, new ArrayList<>(), parentUsers);
        parentUsers.add(new ProjectUser(0L, null, null, null, null, roleParent, null, "user0@email.com"));
        parentUsers.add(new ProjectUser(1L, null, null, null, null, roleParent, null, "user1@email.com"));

        // Define a child role with a few users
        final List<ProjectUser> childUsers = new ArrayList<>();
        final Role roleChild = new Role(idChild, "child", roleParent, new ArrayList<>(), childUsers);
        childUsers.add(new ProjectUser(0L, null, null, null, null, roleChild, null, "user2@email.com"));
        childUsers.add(new ProjectUser(1L, null, null, null, null, roleChild, null, "user3@email.com"));

        // Define the expected result: all accesses, from child and its parent
        final List<ProjectUser> expected = new ArrayList<>();
        expected.addAll(parentUsers);
        expected.addAll(childUsers);

        // Mock
        Mockito.when(roleRepository.exists(idChild)).thenReturn(true);
        Mockito.when(roleRepository.findOne(idChild)).thenReturn(roleChild);

        // Define actual result
        final List<ProjectUser> actual = roleService.retrieveRoleProjectUserList(idChild);

        // Check
        Assert.assertTrue(expected.containsAll(actual) && actual.containsAll(expected));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(idChild);
    }

    /**
     * Check that the system allows to retrieve all resources accesses from the role hierarchy.
     *
     * @throws EntityNotFoundException
     *             thrown when no entity of passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses from the role hierarchy.")
    public void retrieveRoleResourcesAccessList() throws EntityNotFoundException {
        final Long idParent = 0L;
        final Long idChild = 1L;

        // Define a parent role with a few resource accesses
        final List<ResourcesAccess> parentAcceses = new ArrayList<>();
        parentAcceses.add(new ResourcesAccess(0L, "desc0", "mic0", "res0", HttpVerb.TRACE));
        parentAcceses.add(new ResourcesAccess(1L, "desc1", "mic1", "res1", HttpVerb.DELETE));
        final Role roleParent = new Role(idParent, "parent", null, parentAcceses, new ArrayList<>());

        // Define a child role with a few resource accesses and parentRole as its parent role
        final List<ResourcesAccess> childAcceses = new ArrayList<>();
        childAcceses.add(new ResourcesAccess(2L, "desc2", "mic2", "res2", HttpVerb.GET));
        childAcceses.add(new ResourcesAccess(3L, "desc3", "mic3", "res3", HttpVerb.POST));
        final Role roleChild = new Role(idChild, "child", roleParent, childAcceses, new ArrayList<>());

        // Define the expected result: all accesses, from child and its parent
        final List<ResourcesAccess> expected = new ArrayList<>();
        expected.addAll(parentAcceses);
        expected.addAll(childAcceses);

        // Mock
        Mockito.when(roleRepository.exists(idChild)).thenReturn(true);
        Mockito.when(roleRepository.findOne(idChild)).thenReturn(roleChild);

        // Define actual result
        final List<ResourcesAccess> actual = roleService.retrieveRoleResourcesAccessList(idChild);

        // Check
        Assert.assertTrue(expected.containsAll(actual) && actual.containsAll(expected));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(idChild);
    }

    /**
     * Check that the system is able to hierarchically compare two roles.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system is able to hierarchically compare two roles.")
    public void isHierarchicallyInferior() {
        // Init default roles
        final Role rolePublic = new Role(0L, "Plublic", null, null, null);
        final Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, null);
        final Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, null, null);
        final Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, null, null);

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
