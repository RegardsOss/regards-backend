/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.service.role.RoleComparator;
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

    private IAuthenticationResolver authResolver;

    private Role roleRegisteredUser;

    private Role roleAdmin;

    private Role roleProjectAdmin;

    private Role adminSon;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        authResolver = Mockito.mock(IAuthenticationResolver.class);
        roleRepository = Mockito.mock(IRoleRepository.class);
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        roleService = new RoleService("rs-test", roleRepository, projectUserRepository, tenantResolver,
                runtimeTenantResolver, Mockito.mock(IInstancePublisher.class), Mockito.mock(IPublisher.class),
                authResolver);

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

        Mockito.when(roleRepository.findOneById(PUBLIC_ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneById(REGISTERED_USER_ID)).thenReturn(roleRegisteredUser);
        Mockito.when(roleRepository.findOneById(ADMIN_ID)).thenReturn(roleAdmin);
        Mockito.when(roleRepository.findOneById(ADMIN_SON_ID)).thenReturn(adminSon);
        Mockito.when(roleRepository.findOneById(PROJECT_ADMIN_ID)).thenReturn(roleProjectAdmin);

        Mockito.when(roleRepository.findByName(NAME)).thenReturn(Optional.of(rolePublic));
    }

    /**
     * Check that the allows to retrieve roles.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {

        Mockito.when(authResolver.getRole()).thenReturn(null);

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

        Mockito.when(authResolver.getUser()).thenReturn("test@test.test");

        // mock project user
        final ProjectUser projectUser = new ProjectUser("test@test.test", roleAdmin, new ArrayList<>(),
                new ArrayList<>());
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

    // FIXME delete
    // /**
    // * Check that the system fails when trying to create an already existing role.
    // *
    // * @throws EntityAlreadyExistsException
    // * @throws EntityAlreadyExistsException
    // * Thrown if a role with passed id already exists
    // */
    // @Test(expected = EntityAlreadyExistsException.class)
    // @Requirement("REGARDS_DSL_ADM_ADM_210")
    // @Purpose("Check that the system fails when trying to create an already existing role.")
    // public void createRoleDuplicate() throws EntityAlreadyExistsException {
    // Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));
    //
    // final Role duplicate = new Role(NAME, null);
    // roleService.createRole(duplicate);
    // }
    //
    // /**
    // * Check that the system allows to create a role in a regular case.
    // *
    // * @throws EntityAlreadyExistsException
    // * Thrown if a role with passed id already exists
    // */
    // @Test
    // @Requirement("REGARDS_DSL_ADM_ADM_210")
    // @Purpose("Check that the system allows to create a role in a regular case.")
    // public void createRole() throws EntityAlreadyExistsException {
    // final Long id = 4834848L;
    // final Role expected = new Role();
    // expected.setId(id);
    // Mockito.when(roleRepository.save(expected)).thenReturn(expected);
    //
    // Mockito.when(roleRepository.findOneByName(Mockito.anyString())).thenReturn((Optional.empty()));
    // final Role actual = roleService.createRole(expected);
    // Mockito.when(roleRepository.findOneByName(Mockito.anyString())).thenReturn((Optional.of(actual)));
    // Mockito.when(roleRepository.findOne(id)).thenReturn(actual);
    //
    // // Check that the expected and actual role have same values
    // checkRolesEqual(expected, actual);
    //
    // // Check that the repository's method was called with right arguments
    // Mockito.verify(roleRepository).save(expected);
    // }

    /**
     * Check that the system allows to create a role in a regular case.
     *
     * @throws EntityAlreadyExistsException
     *             Thrown if a role with passed id already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role from an other role to copy resources.")
    public void createRoleWithNativeParentPermission() throws EntityException {

        final Role newRole = new Role("newRole", adminSon);

        final Long id = 4834848L;
        final Role expected = new Role();
        expected.setId(id);
        expected.setName("newRole");
        expected.setParentRole(roleAdmin);

        Mockito.when(roleRepository.save(expected)).thenReturn(expected);

        Mockito.when(roleRepository.findOneByName("newRole")).thenReturn((Optional.empty()));
        Mockito.when(roleRepository.findOneByName(roleAdmin.getName())).thenReturn(Optional.of(roleAdmin));
        Mockito.when(roleRepository.findOneByName(adminSon.getName())).thenReturn(Optional.of(adminSon));

        final Role actual = roleService.createRole(newRole);

        Mockito.when(roleRepository.findOneByName(Mockito.anyString())).thenReturn((Optional.of(actual)));
        Mockito.when(roleRepository.findOne(id)).thenReturn(actual);

        // Check that the expected and actual role have same values
        checkRolesEqual(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(roleRepository).save(Mockito.refEq(expected));
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
        final Role notExistent = new Role();
        notExistent.setName("roleName");
        Mockito.when(roleRepository.findByName(notExistent.getName())).thenReturn(Optional.empty());

        roleService.updateRole(notExistent.getName(), notExistent);
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
        final String name = "TheRoleName";
        final Role role = new Role();
        role.setName("roleName");
        Mockito.when(roleRepository.findOneByName(role.getName())).thenReturn(Optional.of(role));

        roleService.updateRole(name, role);
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
        Mockito.when(roleRepository.findByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));

        // Change something on the role
        rolePublic.setAuthorizedAddresses(Lists.newArrayList("0.0.0.0", "127.0.0.1"));

        // Do the update
        roleService.updateRole(NAME, rolePublic);

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
     * @throws EntityException
     */
    @Test(expected = EntityOperationForbiddenException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system does not remove a native role.")
    public void removeRoleNative() throws EntityException {
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
     * @throws EntityException
     *             when the updated role is native. Native roles should not be modified.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role in a regular case.")
    public void removeRole() throws EntityException {
        final Long id = 0L;

        Mockito.when(roleRepository.exists(id)).thenReturn(true);

        Role role = new Role();
        role.setId(id);
        Mockito.when(roleRepository.findOne(id)).thenReturn(role);
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
     * @throws EntityException
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system fails when trying to update permissions of a role which does not exist.")
    public void updateRoleResourcesAccessNotExistent() throws EntityException {
        final Long id = 44255L;

        Mockito.when(roleRepository.exists(id)).thenReturn(false);
        Assert.assertTrue(!roleService.existRole(id));

        final Set<ResourcesAccess> resourcesAccesses = new HashSet<>();
        roleService.updateRoleResourcesAccess(id, resourcesAccesses);
    }

    /**
     * Check that the system allows to add resources accesses on a role.
     *
     * @throws EntityException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210") // FIXME: change the requirement to PM003, ask to claire the naming
    @Purpose("Check that the system allows to add resources accesses on a role.")
    public void updateRoleResourcesAccessAddingResourcesAccess() throws EntityException {
        // Mock
        final Set<ResourcesAccess> resourcesAccesses = new HashSet<>();
        final ResourcesAccess addedResourcesAccess = new ResourcesAccess(468645L, "", "", "", "Controller",
                RequestMethod.PATCH, DefaultRole.ADMIN);
        resourcesAccesses.add(addedResourcesAccess);
        // for this test, let's consider that the user adding a right onto role PUBLIC has the role ADMIN

        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.ADMIN.toString());
        // As ADMIN is the considered role of the caller, we have to add those resources to ADMIN so ADMIN can add those
        // to the desired role
        roleAdmin.setPermissions(resourcesAccesses);

        // mock the hierarchy done into init(PUBLIC <- REGISTERED USER <- ADMIN)
        Mockito.when(roleRepository.findByParentRoleName(rolePublic.getName())).thenAnswer(pInvocation -> {
            final Set<Role> sonsOfPublic = new HashSet<>();
            sonsOfPublic.add(roleRegisteredUser);
            sonsOfPublic.add(new Role("TEST", rolePublic));
            return sonsOfPublic;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleRegisteredUser.getName())).thenAnswer(pInvocation -> {
            final Set<Role> sonsOfRU = new HashSet<>();
            sonsOfRU.add(roleAdmin);
            return sonsOfRU;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleAdmin.getName())).thenAnswer(pInvocation -> {
            final Set<Role> sonsOfAdmin = new HashSet<>();
            return sonsOfAdmin;
        });
        rolePublic.addPermission(new ResourcesAccess(4567L, "", "", "", "Controller", RequestMethod.GET,
                DefaultRole.ADMIN));
        Mockito.when(roleRepository.exists(PUBLIC_ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));
        Mockito.when(roleRepository.save(rolePublic)).thenReturn(rolePublic);
        // because we consider that ADMIN is the role of the caller, we have to be able to return it
        Mockito.when(roleRepository.findByName(DefaultRole.ADMIN.name())).thenReturn(Optional.ofNullable(roleAdmin));

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
     * @throws EntityException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210") // FIXME: change the requirement to PM003, ask to claire the naming
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccessUpdatingResourcesAccess() throws EntityException {
        final Set<ResourcesAccess> initRAs = new HashSet<>();
        initRAs.add(new ResourcesAccess(0L, "desc", "mic", "res", "Controller", RequestMethod.TRACE,
                DefaultRole.ADMIN));

        final Set<ResourcesAccess> passedRAs = new HashSet<>();
        passedRAs.add(new ResourcesAccess(0L, "new desc", "new mic", "new res", "Controller", RequestMethod.DELETE,
                DefaultRole.ADMIN));

        // for this test, let's consider that the user adding a right onto role PUBLIC has the role ADMIN
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.ADMIN.toString());
        // so lets add initRas to PUBLIC...
        rolePublic.setPermissions(initRAs);
        // ... and lets add to ADMIN the right that PUBLIC already has and the one we are adding
        roleAdmin.setPermissions(initRAs);
        roleAdmin.getPermissions().addAll(passedRAs);
        // mock the hierarchy done into init(PUBLIC <- REGISTERED USER <- ADMIN <- PROJECT ADMIN)
        Mockito.when(roleRepository.findByParentRoleName(rolePublic.getName())).thenAnswer(pInvocation -> {
            final Set<Role> sonsOfPublic = new HashSet<>();
            sonsOfPublic.add(roleRegisteredUser);
            return sonsOfPublic;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleRegisteredUser.getName())).thenAnswer(pInvocation -> {
            final Set<Role> sonsOfRU = new HashSet<>();
            sonsOfRU.add(roleAdmin);
            return sonsOfRU;
        });
        Mockito.when(roleRepository.findByParentRoleName(roleAdmin.getName())).thenAnswer(pInvocation -> {
            final Set<Role> sonsOfAdmin = new HashSet<>();
            sonsOfAdmin.add(roleProjectAdmin);
            return sonsOfAdmin;
        });
        Mockito.when(roleRepository.exists(PUBLIC_ID)).thenReturn(true);
        Mockito.when(roleRepository.findOne(PUBLIC_ID)).thenReturn(rolePublic);
        Mockito.when(roleRepository.findOneByName(NAME)).thenReturn(Optional.ofNullable(rolePublic));
        // because we consider that ADMIN is the role of the caller, we have to be able to return it
        Mockito.when(roleRepository.findByName(DefaultRole.ADMIN.name())).thenReturn(Optional.ofNullable(roleAdmin));

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
        resourcesAccesses.add(new ResourcesAccess(0L, "desc", "mic", "res", "Controller", RequestMethod.TRACE,
                DefaultRole.ADMIN));
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
        RoleFactory factory = new RoleFactory();
        Role admin = factory.createAdmin();
        Role projectAdmin = factory.createProjectAdmin();
        Role publicR = factory.createPublic();
        Role registeredUser = factory.createRegisteredUser();

        // lets check that public is inferior to everyone
        Assert.assertFalse(roleService.isHierarchicallyInferior(publicR, publicR));
        Assert.assertTrue(roleService.isHierarchicallyInferior(publicR, registeredUser));
        Assert.assertTrue(roleService.isHierarchicallyInferior(publicR, admin));
        Assert.assertTrue(roleService.isHierarchicallyInferior(publicR, projectAdmin));

        // lets check that registeredUser is only inferior to public
        Assert.assertFalse(roleService.isHierarchicallyInferior(registeredUser, publicR));
        Assert.assertFalse(roleService.isHierarchicallyInferior(registeredUser, registeredUser));
        Assert.assertTrue(roleService.isHierarchicallyInferior(registeredUser, admin));
        Assert.assertTrue(roleService.isHierarchicallyInferior(registeredUser, projectAdmin));

        // check admin
        Assert.assertFalse(roleService.isHierarchicallyInferior(admin, publicR));
        Assert.assertFalse(roleService.isHierarchicallyInferior(admin, registeredUser));
        Assert.assertFalse(roleService.isHierarchicallyInferior(admin, admin));
        Assert.assertTrue(roleService.isHierarchicallyInferior(admin, projectAdmin));

        // check project admin
        Assert.assertFalse(roleService.isHierarchicallyInferior(projectAdmin, publicR));
        Assert.assertFalse(roleService.isHierarchicallyInferior(projectAdmin, registeredUser));
        Assert.assertFalse(roleService.isHierarchicallyInferior(projectAdmin, admin));
        Assert.assertFalse(roleService.isHierarchicallyInferior(projectAdmin, projectAdmin));

        // lets check with a custom role
        Role adminSon = new Role("Admin son", admin);
        Assert.assertFalse(roleService.isHierarchicallyInferior(adminSon, publicR));
        Assert.assertFalse(roleService.isHierarchicallyInferior(adminSon, registeredUser));
        Assert.assertFalse(roleService.isHierarchicallyInferior(adminSon, admin));
        Assert.assertTrue(roleService.isHierarchicallyInferior(adminSon, projectAdmin));
        // a native role is inferior to one of its children
        Assert.assertTrue(roleService.isHierarchicallyInferior(admin, adminSon));

        // native role are not inferior to themselves, lets check for customs
        Assert.assertFalse(roleService.isHierarchicallyInferior(adminSon, adminSon));

        // lets check two custom role on different hierarchical level
        Role registeredUserSon = new Role("Registered User Son", registeredUser);
        Assert.assertFalse(roleService.isHierarchicallyInferior(adminSon, registeredUserSon));
        Assert.assertTrue(roleService.isHierarchicallyInferior(registeredUserSon, adminSon));

        // what happens if a custom role and a native one has the same parent:
        Assert.assertFalse(roleService.isHierarchicallyInferior(admin, registeredUserSon));
        Assert.assertTrue(roleService.isHierarchicallyInferior(registeredUserSon, admin));

        // lets check two custom role on same hierarchical level
        Role adminSon2 = new Role("Admin Son 2", admin);
        Assert.assertTrue(roleService.isHierarchicallyInferior(adminSon, adminSon2));
        Assert.assertTrue(roleService.isHierarchicallyInferior(adminSon2, adminSon));

    }

    @Test
    public void testRoleComparator() {
        // Init default roles
        RoleFactory factory = new RoleFactory();
        Role admin = factory.createAdmin();
        Role projectAdmin = factory.createProjectAdmin();
        Role publicR = factory.createPublic();
        Role registeredUser = factory.createRegisteredUser();
        RoleComparator roleComparator = new RoleComparator(roleService);

        Assert.assertEquals(0, roleComparator.compare(publicR, publicR));
        Assert.assertEquals(-1, roleComparator.compare(publicR, registeredUser));
        Assert.assertEquals(-1, roleComparator.compare(publicR, admin));
        Assert.assertEquals(-1, roleComparator.compare(publicR, projectAdmin));

        Assert.assertEquals(1, roleComparator.compare(registeredUser, publicR));
        Assert.assertEquals(0, roleComparator.compare(registeredUser, registeredUser));
        Assert.assertEquals(-1, roleComparator.compare(registeredUser, admin));
        Assert.assertEquals(-1, roleComparator.compare(registeredUser, projectAdmin));

        Assert.assertEquals(1, roleComparator.compare(admin, publicR));
        Assert.assertEquals(1, roleComparator.compare(admin, registeredUser));
        Assert.assertEquals(0, roleComparator.compare(admin, admin));
        Assert.assertEquals(-1, roleComparator.compare(admin, projectAdmin));

        Assert.assertEquals(1, roleComparator.compare(projectAdmin, publicR));
        Assert.assertEquals(1, roleComparator.compare(projectAdmin, registeredUser));
        Assert.assertEquals(1, roleComparator.compare(projectAdmin, admin));
        Assert.assertEquals(0, roleComparator.compare(projectAdmin, projectAdmin));

        // lets check with a custom role
        Role adminSon = new Role("Admin Son", admin);

        Assert.assertEquals(1, roleComparator.compare(adminSon, publicR));
        Assert.assertEquals(1, roleComparator.compare(adminSon, registeredUser));
        Assert.assertEquals(1, roleComparator.compare(adminSon, admin));
        Assert.assertEquals(-1, roleComparator.compare(adminSon, projectAdmin));
        // a native role is inferior to one of its children
        Assert.assertEquals(-1, roleComparator.compare(admin, adminSon));

        // lets check two custom role on different hierarchical level
        Role registeredUserSon = new Role("Registered User Son", registeredUser);

        Assert.assertEquals(1, roleComparator.compare(adminSon, registeredUserSon));
        Assert.assertEquals(-1, roleComparator.compare(registeredUserSon, adminSon));

        // what happens if a custom role and a native one has the same parent:
        Assert.assertEquals(1, roleComparator.compare(admin, registeredUserSon));
        Assert.assertEquals(-1, roleComparator.compare(registeredUserSon, admin));

        // lets check two custom role on same hierarchical level
        Role adminSon2 = new Role("Admin Son 2", admin);

        // admin son is considered
        Assert.assertEquals(-1, roleComparator.compare(adminSon, adminSon2));
        Assert.assertEquals(1, roleComparator.compare(adminSon2, adminSon));
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
