/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.LocalTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Test class for {@link RoleService}.
 *
 * @author xbrochar
 * @author Sébastien Binda
 */
public class RoleServiceTest {

    /**
     * The role public
     */
    private static Role rolePublic;

    /**
     * The role public id
     */
    private static final Long ID = 0L;

    /**
     * The role public name
     */
    private static final String NAME = DefaultRole.PUBLIC.toString();

    /**
     * The tested service
     */
    private IRoleService roleService;

    /**
     * Mock repository
     */
    private IRoleRepository roleRepository;

    /**
     * Mock repository
     */
    private IProjectUserRepository projectUserRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        roleRepository = Mockito.mock(IRoleRepository.class);
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        final JWTService jwService = new JWTService();
        jwService.setSecret("123456789");
        roleService = new RoleService("rs-test", roleRepository, projectUserRepository, new LocalTenantResolver(),
                jwService, null);

        // Clear the repos
        projectUserRepository.deleteAll();
        roleRepository.deleteAll();
        rolePublic = new Role(NAME, null);
        // Set an id in order to simulate it was saved in db
        rolePublic.setId(ID);
    }

    /**
     * Check that the allows to retrieve roles.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        final List<Role> expected = new ArrayList<>();
        expected.add(rolePublic);

        Mockito.when(roleRepository.findAll()).thenReturn(expected);
        final List<Role> actual = roleService.retrieveRoleList();

        // Check that the expected and actual role have same values
        checkRolesEqual(expected.get(0), actual.get(0));
        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findAll();
    }

    /**
     * Check that the allows to retrieve a single role.
     *
     * @throws EntityNotFoundException
     *             when no role with passed name could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role.")
    public void retrieveRole() throws EntityNotFoundException {
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));
        final Role actual = roleService.retrieveRole(NAME);

        // Check that the expected and actual role have same values
        checkRolesEqual(rolePublic, actual);
        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOneByName(NAME);
    }

    /**
     * Check that the system fails when trying to create an already existing role.
     *
     * @throws EntityAlreadyExistsException
     *
     * @throws EntityAlreadyExistsException
     *             Thrown if a role with passed id already exists
     */
    @Test(expected = EntityAlreadyExistsException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to create an already existing role.")
    public void createRole_duplicate() throws EntityAlreadyExistsException {
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        final Role duplicate = new Role(NAME, null);
        roleService.createRole(duplicate);
    }

    /**
     * Check that the system allows to create a role in a regular case.
     *
     * @throws EntityAlreadyExistsException
     *             Thrown if a role with passed id already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role in a regular case.")
    public void createRole() throws EntityAlreadyExistsException {
        final Long id = 4834848L;
        final Role expected = new Role();
        expected.setId(id);
        Mockito.when(roleRepository.save(expected)).thenReturn(expected);

        Mockito.when(roleRepository.findOneByName(Mockito.anyString())).thenReturn((Optional.empty()));
        final Role actual = roleService.createRole(expected);
        Mockito.when(roleRepository.findOneByName(Mockito.anyString())).thenReturn((Optional.of(actual)));
        Mockito.when(roleRepository.findOne(id)).thenReturn(actual);

        // Check that the expected and actual role have same values
        checkRolesEqual(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(expected);
    }

    /**
     * Check that the system fails when trying to update a role which does not exist.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br/>
     *             <br>
     *             {@link EntityInconsistentIdentifierException} Thrown if passed role id differs from the id of the
     *             passed role<br>
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which does not exist.")
    public void updateRoleNotExistent() throws EntityException {
        final Long id = 58354L;
        final Role notExistent = new Role();
        notExistent.setId(id);
        Mockito.when(roleRepository.exists(id)).thenReturn(false);

        roleService.updateRole(id, notExistent);
    }

    /**
     * Check that the system fails when trying to update a role which id is different from the passed one.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br/>
     *             <br>
     *             {@link EntityInconsistentIdentifierException} Thrown if passed role id differs from the id of the
     *             passed role<br>
     */
    @Test(expected = EntityInconsistentIdentifierException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update a role which id is different from the passed one.")
    public void updateRoleWrongId() throws EntityException {
        final Long id = 58354L;
        final Role role = new Role();
        role.setId(99L);
        Assert.assertTrue(!id.equals(role.getId()));

        roleService.updateRole(id, role);
    }

    /**
     * Check that the system allows to update a role in a regular case.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br/>
     *             <br>
     *             {@link EntityInconsistentIdentifierException} Thrown if passed role id differs from the id of the
     *             passed role<br>
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role in a regular case.")
    public void updateRole() throws EntityException {
        // Mock
        Mockito.when(roleRepository.exists(ID)).thenReturn(true);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        // Change something on the role
        rolePublic.setCorsRequestsAuthorized(!rolePublic.isCorsRequestsAuthorized());

        // Do the update
        roleService.updateRole(ID, rolePublic);

        // Retrieve the updated role
        Mockito.when(roleRepository.findOne(ID)).thenReturn(rolePublic);
        final Role updatedRole = roleService.retrieveRole(NAME);

        // Ensure they are now equal
        checkRolesEqual(rolePublic, updatedRole);

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(rolePublic);
    }

    /**
     * Check that the system does not remove a native role.
     *
     * @throws EntityOperationForbiddenException
     *             when the updated role is native. Native roles should not be modified.
     */
    @Test(expected = EntityOperationForbiddenException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system does not remove a native role.")
    public void removeRoleNative() throws EntityOperationForbiddenException {
        final Long id = 0L;
        final RoleFactory roleFactory = new RoleFactory();
        final Role roleNative = roleFactory.createPublic();

        // Mock repo
        Mockito.when(roleRepository.exists(id)).thenReturn(true);
        Mockito.when(roleRepository.findOne(id)).thenReturn(roleNative);

        // Call tested method
        roleService.removeRole(id);
    }

    /**
     * Check that the system allows to delete a role in a regular case.
     *
     * @throws EntityOperationForbiddenException
     *             when the updated role is native. Native roles should not be modified.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role in a regular case.")
    public void removeRole() throws EntityOperationForbiddenException {
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
        // Mock
        Mockito.when(roleRepository.exists(ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));
        Assert.assertTrue(roleService.existRole(ID));

        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        final ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645L, "", "", "", HttpVerb.PATCH);
        resourcesAccesses.add(addedResourcesAccess);

        // Perform the update
        roleService.updateRoleResourcesAccess(ID, resourcesAccesses);

        // Prepare the expected result
        final Role expected = new Role(NAME, null);
        expected.setPermissions(resourcesAccesses);
        Mockito.when(roleRepository.findOne(ID)).thenReturn(expected);

        // Check
        Assert.assertTrue(roleService.retrieveRole(NAME).getPermissions().contains(addedResourcesAccess));

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(Mockito.refEq(expected, "id"));
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
        final Role role = new Role(NAME, null);

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
        Role updatedRole = new Role(NAME, null);
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
        // Prepare the role by adding some resources accesses
        final List<ResourcesAccess> resourcesAccesses = new ArrayList<>();
        resourcesAccesses.add(new ResourcesAccess(0L, "desc", "mic", "res", HttpVerb.TRACE));
        rolePublic.setPermissions(resourcesAccesses);

        // Mock
        Mockito.when(roleRepository.exists(ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        roleService.clearRoleResourcesAccess(ID);

        // Retrieve updated role
        final Role updated = roleService.retrieveRole(NAME);

        // Check
        Assert.assertTrue(updated.getPermissions().isEmpty());

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(rolePublic);
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
        final String roleParentName = "parent";
        final String roleChildName = "child";

        // Define a parent role with a few users
        final List<ProjectUser> parentUsers = new ArrayList<>();
        final Role roleParent = new Role(roleParentName, null);
        parentUsers.add(new ProjectUser("user0@email.com", roleParent, null, null));
        parentUsers.add(new ProjectUser("user1@email.com", roleParent, null, null));

        // Define a child role with a few users
        final List<ProjectUser> childUsers = new ArrayList<>();
        final Role roleChild = new Role(roleChildName, roleParent);
        childUsers.add(new ProjectUser("user2@email.com", roleChild, null, null));
        childUsers.add(new ProjectUser("user3@email.com", roleChild, null, null));

        // Define the expected result: all accesses, from child and its parent
        final List<ProjectUser> expected = new ArrayList<>();
        expected.addAll(parentUsers);
        expected.addAll(childUsers);

        // Mock
        Mockito.when(roleRepository.exists(idParent)).thenReturn(true);
        Mockito.when(roleRepository.findOne(idParent)).thenReturn(roleParent);

        final List<String> roleNames = new ArrayList<>();
        roleNames.add(roleChildName);
        roleNames.add(roleParentName);

        final List<Role> inehtitedRoleOfParentRole = new ArrayList<>();
        inehtitedRoleOfParentRole.add(roleChild);
        final Page<ProjectUser> pageExpected = new PageImpl<>(expected);

        final Pageable pageable = new PageRequest(0, 100);
        Mockito.when(roleRepository.findByParentRoleName(roleParentName)).thenReturn(inehtitedRoleOfParentRole);
        Mockito.when(projectUserRepository.findByRoleNameIn(roleNames, pageable)).thenReturn(pageExpected);

        final Page<ProjectUser> expectedPage = new PageImpl<>(expected);

        // Define actual result
        final Page<ProjectUser> actual = roleService.retrieveRoleProjectUserList(idParent, pageable);

        // Check
        Assert.assertEquals(expectedPage, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findOne(idParent);
        Mockito.verify(projectUserRepository).findByRoleNameIn(roleNames, pageable);
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
        final Role roleParent = new Role("parent", null);
        roleParent.setPermissions(parentAcceses);

        // Define a child role with a few resource accesses and parentRole as its parent role
        final List<ResourcesAccess> childAcceses = new ArrayList<>();
        childAcceses.add(new ResourcesAccess(2L, "desc2", "mic2", "res2", HttpVerb.GET));
        childAcceses.add(new ResourcesAccess(3L, "desc3", "mic3", "res3", HttpVerb.POST));
        final Role roleChild = new Role("child", roleParent);
        roleChild.setPermissions(childAcceses);

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
        final RoleFactory factory = new RoleFactory();
        final Role roleInstanceAdmin = factory.createInstanceAdmin();
        final Role roleProjectAdmin = roleInstanceAdmin.getParentRole();
        final Role roleAdmin = roleProjectAdmin.getParentRole();
        final Role roleRegisteredUser = roleAdmin.getParentRole();
        final Role rolePublic = roleRegisteredUser.getParentRole();

        Assert.assertTrue(roleService.isHierarchicallyInferior(roleRegisteredUser, roleProjectAdmin));
        Assert.assertFalse(roleService.isHierarchicallyInferior(roleProjectAdmin, roleRegisteredUser));

        // final Role admin = roleService.retrieveRole(2L);
        final Role customRoleFromAdmin = new Role("custom role", roleAdmin);

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
    }

}
