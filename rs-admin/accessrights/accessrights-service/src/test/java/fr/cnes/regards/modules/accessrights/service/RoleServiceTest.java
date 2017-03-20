/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
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
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @author Sylvain Vissiere-Guerinet
 */
public class RoleServiceTest {

    /**
     * The role public
     */
    private Role rolePublic;

    /**
     * The role public id
     */
    private static final Long PUBLIC_ID = 0L;

    /**
     * The role registered user id
     */
    private static final Long REGISTERED_USER_ID = 1L;

    /**
     * The role admin id
     */
    private static final Long ADMIN_ID = 2L;

    /**
     * The role project admin id
     */
    private static final Long PROJECT_ADMIN_ID = 3L;

    /**
     * The role public name
     */
    private static final String NAME = DefaultRole.PUBLIC.toString();

    private static final Long ADMIN_SON_ID = 4L;

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
     * Tenant resolver
     */
    private ITenantResolver tenantResolver;

    /**
     * Runtime tenant resolver
     */
    private IRuntimeTenantResolver runtimeTenantResolver;

    private Role roleRegisteredUser;

    private Role roleAdmin;

    private Role roleProjectAdmin;

    private JWTService jwtService;

    private Role adminSon;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        roleRepository = Mockito.mock(IRoleRepository.class);
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        jwtService = Mockito.mock(JWTService.class);
        jwtService.setSecret("123456789");
        roleService = new RoleService("rs-test", roleRepository, projectUserRepository, tenantResolver,
                runtimeTenantResolver, jwtService, null);

        // Clear the repos
        projectUserRepository.deleteAll();
        roleRepository.deleteAll();
        rolePublic = new Role(NAME, null);
        rolePublic.setNative(true);
        roleRegisteredUser = new Role(DefaultRole.REGISTERED_USER.toString(), rolePublic);
        roleRegisteredUser.setNative(true);
        roleAdmin = new Role(DefaultRole.ADMIN.toString(), roleRegisteredUser);
        roleAdmin.setNative(true);
        roleProjectAdmin = new Role(DefaultRole.PROJECT_ADMIN.toString(), null);
        roleProjectAdmin.setNative(true);
        adminSon = new Role(DefaultRole.ADMIN.toString() + "_SON", roleAdmin);

        // Set an id in order to simulate it was saved in db
        rolePublic.setId(PUBLIC_ID);
        roleRegisteredUser.setId(REGISTERED_USER_ID);
        roleAdmin.setId(ADMIN_ID);
        roleProjectAdmin.setId(PROJECT_ADMIN_ID);
        adminSon.setId(ADMIN_SON_ID);
        Mockito.when(roleRepository.findOneByName(roleAdmin.getName())).thenReturn(Optional.of(roleAdmin));
    }

    /**
     * Check that the allows to retrieve roles.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        final Set<Role> expected = new HashSet<>();
        expected.add(rolePublic);

        Mockito.when(roleRepository.findAllDistinctLazy()).thenReturn(expected);
        final Set<Role> actual = roleService.retrieveRoles();

        // Check that the expected and actual role have same values
        checkRolesEqual((Role) expected.toArray()[0], (Role) actual.toArray()[0]);
        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).findAllDistinctLazy();
    }

    @Test
    @Requirement("PM003") // FIXME
    @Purpose("Check that the system retrieve the good roles that can be borrowed")
    public void retrieveBorrowableRoles() throws JwtException {
        // mock JWTAuthentication
        JWTAuthentication token = new JWTAuthentication("");
        UserDetails user = new UserDetails();
        user.setName("test@test.test");
        user.setRole("ADMIN");
        token.setUser(user);
        Mockito.when(jwtService.getCurrentToken()).thenReturn(token);
        // mock project user
        ProjectUser projectUser = new ProjectUser("test@test.test", roleAdmin, new ArrayList<>(), new ArrayList<>());
        Mockito.when(projectUserRepository.findOneByEmail("test@test.test")).thenReturn(Optional.of(projectUser));
        Mockito.when(roleRepository.findByParentRoleName(roleAdmin.getName())).thenReturn(Sets.newHashSet(adminSon));
        Set<Role> result = roleService.retrieveBorrowableRoles();
        Assert.assertTrue(result.contains(rolePublic));
        Assert.assertTrue(result.contains(roleRegisteredUser));
        Assert.assertTrue(result.contains(roleAdmin));
        Assert.assertFalse(result.contains(adminSon));
        // PUBLIC cannot borrow roles
        projectUser.setRole(rolePublic);
        result = roleService.retrieveBorrowableRoles();
        Assert.assertTrue(result.isEmpty());
        // PROJECT_ADMIN can borrow all roles except instance_admin
        projectUser.setRole(roleProjectAdmin);
        result = roleService.retrieveBorrowableRoles();
        Assert.assertTrue(result.contains(rolePublic));
        Assert.assertTrue(result.contains(roleRegisteredUser));
        Assert.assertTrue(result.contains(roleAdmin));
        Assert.assertTrue(result.contains(adminSon));
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
    public void createRoleDuplicate() throws EntityAlreadyExistsException {
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
        Mockito.when(roleRepository.exists(PUBLIC_ID)).thenReturn(true);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        // Change something on the role
        rolePublic.setCorsRequestsAuthorized(!rolePublic.isCorsRequestsAuthorized());

        // Do the update
        roleService.updateRole(PUBLIC_ID, rolePublic);

        // Retrieve the updated role
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(rolePublic);
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
     * @throws EntityOperationForbiddenException
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update permissions of a role which does not exist.")
    public void updateRoleResourcesAccessNotExistent()
            throws EntityNotFoundException, EntityOperationForbiddenException {
        final Long id = 44255L;

        Mockito.when(roleRepository.exists(id)).thenReturn(false);
        Assert.assertTrue(!roleService.existRole(id));

        final Set<ResourcesAccess> resourcesAccesses = new HashSet<>();
        roleService.updateRoleResourcesAccess(id, resourcesAccesses);
    }

    /**
     * Check that the system allows to add resources accesses on a role.
     *
     * @throws EntityNotFoundException
     *             Thrown if no role with passed id could be found
     * @throws EntityOperationForbiddenException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210") // FIXME: change the requirement to PM003, ask to claire the naming
    @Purpose("Check that the system allows to add resources accesses on a role.")
    public void updateRoleResourcesAccessAddingResourcesAccess()
            throws EntityNotFoundException, EntityOperationForbiddenException {
        // Mock
        // for this test, let's consider that the user adding a right onto role PUBLIC has the role ADMIN
        SecurityUtils.mockActualRole(DefaultRole.ADMIN.toString());

        // mock the hierarchy done into init(PUBLIC <- REGISTERED USER <- ADMIN)
        Mockito.when(roleRepository.findByParentRoleName(rolePublic.getName())).thenAnswer(pInvocation -> {
            Set<Role> sonsOfPublic = new HashSet<>();
            sonsOfPublic.add(roleRegisteredUser);
            sonsOfPublic.add(new Role("TEST", rolePublic));
            return sonsOfPublic;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleRegisteredUser.getName())).thenAnswer(pInvocation -> {
            Set<Role> sonsOfRU = new HashSet<>();
            sonsOfRU.add(roleAdmin);
            return sonsOfRU;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleAdmin.getName())).thenAnswer(pInvocation -> {
            Set<Role> sonsOfAdmin = new HashSet<>();
            return sonsOfAdmin;
        });
        Mockito.when(roleRepository.exists(PUBLIC_ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));
        Mockito.when(roleRepository.save(rolePublic)).thenReturn(rolePublic);
        Mockito.when(roleRepository.save(roleRegisteredUser)).thenReturn(roleRegisteredUser);
        Mockito.when(roleRepository.save(roleAdmin)).thenReturn(roleAdmin);
        Mockito.when(roleRepository.save(roleProjectAdmin)).thenReturn(roleProjectAdmin);

        final Set<ResourcesAccess> resourcesAccesses = new HashSet<>();
        final ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645L, "", "", "", "Controller",
                HttpVerb.PATCH);
        resourcesAccesses.add(addedResourcesAccess);

        // Perform the update
        roleService.updateRoleResourcesAccess(PUBLIC_ID, resourcesAccesses);

        // Prepare the expected result
        final Role expected = new Role(NAME, null);
        expected.setPermissions(resourcesAccesses);
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(expected);

        // Check
        Assert.assertTrue(roleService.retrieveRole(NAME).getPermissions().contains(addedResourcesAccess));
        // Check that the access has been added to Registered User and Admin too
        Assert.assertTrue(roleRegisteredUser.getPermissions().contains(addedResourcesAccess));
        Assert.assertTrue(roleAdmin.getPermissions().contains(addedResourcesAccess));
    }

    /**
     * Check that the system allows to update resources accesses of a role.
     *
     * @throws EntityNotFoundException
     *             Thrown if no role with passed id could be found
     * @throws EntityOperationForbiddenException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210") // FIXME: change the requirement to PM003, ask to claire the naming
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccessUpdatingResourcesAccess()
            throws EntityNotFoundException, EntityOperationForbiddenException {
        final List<ResourcesAccess> initRAs = new ArrayList<>();
        initRAs.add(new ResourcesAccess(0L, "desc", "mic", "res", "Controller", HttpVerb.TRACE));

        // for this test, let's consider that the user adding a right onto role PUBLIC has the role ADMIN
        SecurityUtils.mockActualRole(DefaultRole.ADMIN.toString());
        // mock the hierarchy done into init(PUBLIC <- REGISTERED USER <- ADMIN <- PROJECT ADMIN)
        Mockito.when(roleRepository.findByParentRoleName(rolePublic.getName())).thenAnswer(pInvocation -> {
            Set<Role> sonsOfPublic = new HashSet<>();
            sonsOfPublic.add(roleRegisteredUser);
            return sonsOfPublic;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleRegisteredUser.getName())).thenAnswer(pInvocation -> {
            Set<Role> sonsOfRU = new HashSet<>();
            sonsOfRU.add(roleAdmin);
            return sonsOfRU;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleAdmin.getName())).thenAnswer(pInvocation -> {
            Set<Role> sonsOfAdmin = new HashSet<>();
            sonsOfAdmin.add(roleProjectAdmin);
            return sonsOfAdmin;
        });
        Mockito.when(roleRepository.exists(PUBLIC_ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        final Set<ResourcesAccess> passedRAs = new HashSet<>();
        passedRAs.add(new ResourcesAccess(0L, "new desc", "new mic", "new res", "Controller", HttpVerb.DELETE));

        // Ensure new permission's attributes are different from the previous

        Assert.assertNotEquals(passedRAs, initRAs);

        // Perform the update
        Role updatedRole = new Role(NAME, null);
        Mockito.when(roleRepository.save(updatedRole)).thenReturn(updatedRole);
        updatedRole = roleService.updateRoleResourcesAccess(PUBLIC_ID, passedRAs);

        // Ensure they are now equal
        final Set<ResourcesAccess> updatedRAs = updatedRole.getPermissions();
        Assert.assertEquals(updatedRAs, passedRAs);
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
        final Set<ResourcesAccess> resourcesAccesses = new HashSet<>();
        resourcesAccesses.add(new ResourcesAccess(0L, "desc", "mic", "res", "Controller", HttpVerb.TRACE));
        rolePublic.setPermissions(resourcesAccesses);

        // Mock
        Mockito.when(roleRepository.exists(PUBLIC_ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        roleService.clearRoleResourcesAccess(PUBLIC_ID);

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
        roleParent.setId(190L);

        // Define a child role with a few users
        final List<ProjectUser> childUsers = new ArrayList<>();
        final Role roleChild = new Role(roleChildName, roleParent);
        childUsers.add(new ProjectUser("user2@email.com", roleChild, null, null));
        childUsers.add(new ProjectUser("user3@email.com", roleChild, null, null));
        roleChild.setId(191L);

        // Define the expected result: all accesses, from child and its parent
        final List<ProjectUser> expected = new ArrayList<>();
        expected.addAll(parentUsers);
        expected.addAll(childUsers);

        // Mock
        Mockito.when(roleRepository.exists(idParent)).thenReturn(true);
        Mockito.when(roleRepository.findOne(idParent)).thenReturn(roleParent);

        final Set<String> roleNames = new HashSet<>();
        roleNames.add(roleChildName);
        roleNames.add(roleParentName);

        final Set<Role> inehtitedRoleOfParentRole = new HashSet<>();
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
     * Check that the system is able to hierarchically compare two roles.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system is able to hierarchically compare two roles.")
    public void isHierarchicallyInferior() {
        // Init default roles
        final RoleFactory factory = new RoleFactory();
        final Role roleInstanceAdmin = factory.createInstanceAdmin();
        final Role roleProjectAdminParent = roleInstanceAdmin.getParentRole();
        final Role roleAdminParent = roleProjectAdminParent.getParentRole();
        final Role roleRegisteredUserParent = roleAdminParent.getParentRole();
        final Role rolePublicParent = roleRegisteredUserParent.getParentRole();

        Assert.assertNotNull(roleInstanceAdmin);
        Assert.assertNotNull(roleProjectAdminParent);
        Assert.assertNotNull(roleAdminParent);
        Assert.assertNotNull(roleRegisteredUserParent);
        Assert.assertNotNull(rolePublicParent);

        Assert.assertTrue(roleService.isHierarchicallyInferior(roleRegisteredUserParent, roleProjectAdminParent));
        Assert.assertFalse(roleService.isHierarchicallyInferior(roleProjectAdminParent, roleRegisteredUserParent));

        // final Role admin = roleService.retrieveRole(2L);
        final Role customRoleFromAdmin = new Role("custom role", roleAdminParent);

        Assert.assertFalse(roleService.isHierarchicallyInferior(customRoleFromAdmin, roleRegisteredUserParent));
        Assert.assertFalse(roleService.isHierarchicallyInferior(customRoleFromAdmin, roleProjectAdminParent));
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
