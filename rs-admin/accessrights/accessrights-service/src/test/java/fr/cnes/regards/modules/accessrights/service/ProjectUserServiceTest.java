/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;

/**
 * Test class for {@link ProjectUserService}.
 * @author xbrochar
 */
public class ProjectUserServiceTest {

    /**
     * A sample id
     */
    private static final Long ID = 0L;

    /**
     * A sample email
     */
    private static final String EMAIL = "user@email.com";

    /**
     * A sample last connection date
     */
    private static final OffsetDateTime LAST_CONNECTION = OffsetDateTime.now().minusDays(2);

    /**
     * A sample last update date
     */
    private static final OffsetDateTime LAST_UPDATE = OffsetDateTime.now().minusHours(1);

    /**
     * A sample status
     */
    private static final UserStatus STATUS = UserStatus.ACCESS_GRANTED;

    /**
     * A sample meta data list
     */
    private static final List<MetaData> META_DATA = new ArrayList<>();

    /**
     * A sample role
     */
    private static final Role ROLE = new Role(DefaultRole.ADMIN.toString(), null);

    /**
     * A sample list of permissions
     */
    private static final List<ResourcesAccess> PERMISSIONS = new ArrayList<>();

    /**
     * A sample project user
     */
    private static ProjectUser projectUser = new ProjectUser();

    /**
     * The tested service
     */
    private IProjectUserService projectUserService;

    /**
     * Mocked CRUD repository managing {@link ProjectUser}s
     */
    private IProjectUserRepository projectUserRepository;

    /**
     * Mocked service handling CRUD operation on {@link Role}s
     */
    private IRoleService roleService;

    private IAccountsClient accountsClient;

    private IAuthenticationResolver authResolver;

    private IUserClient userAccessGroupsClient;

    private IAccessSettingsService accessSettingsService;

    private AccessSettings settings;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        // Initialize a sample user
        projectUser.setId(ID);
        projectUser.setEmail(EMAIL);
        projectUser.setLastConnection(LAST_CONNECTION);
        projectUser.setLastUpdate(LAST_UPDATE);
        projectUser.setStatus(STATUS);
        projectUser.setMetadata(META_DATA);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.getPermissions().add(new ResourcesAccess(0L, "desc0", "ms0", "res0", "Controller",
                RequestMethod.GET, DefaultRole.ADMIN));
        projectUser.getPermissions().add(new ResourcesAccess(1L, "desc1", "ms1", "res1", "Controller",
                RequestMethod.PUT, DefaultRole.ADMIN));
        projectUser.setRole(ROLE);

        // Mock untested services & repos
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        roleService = Mockito.mock(IRoleService.class);
        accountsClient = Mockito.mock(IAccountsClient.class);
        authResolver = Mockito.mock(IAuthenticationResolver.class);
        userAccessGroupsClient = Mockito.mock(IUserClient.class);
        accessSettingsService = Mockito.mock(IAccessSettingsService.class);
        settings = new AccessSettings();
        settings.setDefaultRole(new Role(DefaultRole.PUBLIC.toString()));
        settings.setDefaultGroups(Lists.newArrayList("group1"));
        Mockito.when(accessSettingsService.retrieve()).thenReturn(settings);

        // Construct the tested service
        projectUserService = new ProjectUserService(authResolver, projectUserRepository, roleService, accountsClient,
                userAccessGroupsClient, "instance_admin@regards.fr", accessSettingsService, new Gson());
    }

    @Test
    @Purpose("Check that the system allows to create a new projectUser and the associated account.")
    public void createUserByBypassingRegristrationProcess() throws EntityNotFoundException, EntityInvalidException {
        Mockito.when(accountsClient.retrieveAccounByEmail("test@regards.fr"))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        Mockito.when(projectUserRepository.findOneByEmail("test@regards.fr")).thenReturn(Optional.ofNullable(null));
        Mockito.when(roleService.retrieveRole("roleName")).thenReturn(new Role());
        ProjectUser user = new ProjectUser();
        user.setEmail("test@regards.fr");
        Mockito.when(projectUserRepository.save(Mockito.any(ProjectUser.class))).thenAnswer(args -> {
            return args.getArgument(0, ProjectUser.class);
        });

        final AccessRequestDto accessRequest = new AccessRequestDto("test@regards.fr", "pFirstName", "pLastName", null,
                null, "pPassword", "pOriginUrl", "pRequestLink");

        try {
            ProjectUser newUser = projectUserService.createProjectUser(accessRequest);
            projectUserService.configureAccessGroups(newUser);
            Assert.assertEquals(settings.getDefaultRole(), newUser.getRole());

            // Check group association
            Assert.assertTrue("This test needs some groups configured id default settings",
                              !settings.getDefaultGroups().isEmpty());
            settings.getDefaultGroups().forEach(g -> {
                Mockito.verify(userAccessGroupsClient, Mockito.times(1)).associateAccessGroupToUser(user.getEmail(), g);
            });

            // Check that createAccount method is called
            Mockito.verify(accountsClient).createAccount(Mockito.any());
            Mockito.verify(projectUserRepository).save(Mockito.any(ProjectUser.class));

        } catch (final EntityAlreadyExistsException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Purpose("Check that the system allows to create a new projectUser with the associated account.")
    public void createUserByBypassingRegristrationProcessWithoutAccount()
            throws EntityNotFoundException, EntityInvalidException {
        Mockito.when(accountsClient.retrieveAccounByEmail("test@regards.fr"))
                .thenReturn(new ResponseEntity<>(
                        new EntityModel<>(new Account("test@regards.fr", "pFirstName", "pLastName", "pPassword")),
                        HttpStatus.OK));
        Mockito.when(projectUserRepository.findOneByEmail("test@regards.fr")).thenReturn(Optional.ofNullable(null));
        Mockito.when(roleService.retrieveRole("roleName")).thenReturn(new Role());
        ProjectUser user = new ProjectUser();
        user.setEmail("test@regards.fr");
        Mockito.when(projectUserRepository.save(Mockito.any())).thenReturn(user);

        final AccessRequestDto accessRequest = new AccessRequestDto("test@regards.fr", "pFirstName", "pLastName",
                "roleName", null, "pPassword", "pOriginUrl", "pRequestLink");

        try {
            ProjectUser newuser = projectUserService.createProjectUser(accessRequest);
            projectUserService.configureAccessGroups(newuser);

            // Check group association
            settings.getDefaultGroups().forEach(g -> {
                Mockito.verify(userAccessGroupsClient, Mockito.times(1)).associateAccessGroupToUser(user.getEmail(), g);
            });

            // Chcek that createAccount method is called
            Mockito.verify(accountsClient, Mockito.never()).createAccount(Mockito.any());
            Mockito.verify(projectUserRepository).save(Mockito.any(ProjectUser.class));

        } catch (final EntityAlreadyExistsException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Purpose("Check that the system allows to create a new projectUser with the associated account.")
    public void createUserByBypassingRegristrationProcessError()
            throws EntityNotFoundException, EntityInvalidException {

        Mockito.when(accountsClient.retrieveAccounByEmail("test@regards.fr"))
                .thenReturn(new ResponseEntity<>(
                        new EntityModel<>(new Account("test@regards.fr", "pFirstName", "pLastName", "pPassword")),
                        HttpStatus.OK));
        Mockito.when(projectUserRepository.findOneByEmail("test@regards.fr"))
                .thenReturn(Optional.of(new ProjectUser()));
        Mockito.when(roleService.retrieveRole("roleName")).thenReturn(new Role());

        final AccessRequestDto accessRequest = new AccessRequestDto("test@regards.fr", "pFirstName", "pLastName",
                "roleName", null, "pPassword", "pOriginUrl", "pRequestLink");

        try {
            projectUserService.createProjectUser(accessRequest);
            Assert.fail("ProjectUser already exists. There should be an exception thronw here.");
        } catch (final EntityAlreadyExistsException e) {
            // Nothing to do. There should be an exception
        }
    }

    /**
     * Check that the system allows to retrieve the users of a project.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the users of a project.")
    public void retrieveUserListByStatus() {
        // Define expected
        final List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);

        final Pageable pageable = PageRequest.of(0, 100);
        final Page<ProjectUser> expectedPage = new PageImpl<>(expected, pageable, 1);

        // Mock the repository returned value
        Mockito.when(projectUserRepository.findAll(Mockito.any(Specification.class), Mockito.eq(pageable)))
                .thenReturn(expectedPage);

        // Retrieve actual value
        final Page<ProjectUser> actual = projectUserService.retrieveUserList(UserStatus.ACCESS_GRANTED.toString(), null,
                                                                             pageable);

        // Check that the expected and actual role have same values
        Assert.assertEquals(expectedPage, actual);
    }

    /**
     * Check that the system allows to retrieve the users of a project.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the users of a project.")
    public void retrieveAllUserList() {
        // Define expected
        final List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);

        final Pageable pageable = PageRequest.of(0, 100);
        final Page<ProjectUser> expectedPage = new PageImpl<>(expected, pageable, 1);

        // Mock the repository returned value
        Mockito.when(projectUserRepository.findAll(Mockito.any(Specification.class), Mockito.eq(pageable)))
                .thenReturn(expectedPage);

        // Retrieve actual value
        final Page<ProjectUser> actual = projectUserService.retrieveUserList(null, null, pageable);

        // Check that the expected and actual role have same values
        Assert.assertEquals(expectedPage, actual);
    }

    /**
     * Check that the system allows to retrieve a specific user without exposing hidden meta data.
     * @throws EntityNotFoundException When no user with passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve a specific user without exposing hidden meta data.")
    public void retrieveUser() throws EntityNotFoundException {
        // Define user as in db
        final MetaData metaData0 = new MetaData();
        metaData0.setVisibility(UserVisibility.HIDDEN);
        projectUser.getMetadata().add(metaData0);
        final MetaData metaData1 = new MetaData();
        metaData1.setVisibility(UserVisibility.READABLE);
        projectUser.getMetadata().add(metaData1);
        final MetaData metaData2 = new MetaData();
        metaData2.setVisibility(UserVisibility.WRITEABLE);
        projectUser.getMetadata().add(metaData2);

        // Define user as expected
        final ProjectUser expected = new ProjectUser();
        final List<MetaData> visibleMetaData = new ArrayList<>();
        visibleMetaData.add(metaData1);
        visibleMetaData.add(metaData2);
        expected.setId(ID);
        expected.setEmail(EMAIL);
        expected.setLastUpdate(LAST_UPDATE);
        expected.setLastConnection(LAST_CONNECTION);
        expected.setStatus(STATUS);
        expected.setPermissions(PERMISSIONS);
        expected.setRole(ROLE);
        expected.setMetadata(visibleMetaData);

        // Mock the repository returned value
        Mockito.when(projectUserRepository.findById(ID)).thenReturn(Optional.of(projectUser));

        // Retrieve actual value
        final ProjectUser actual = projectUserService.retrieveUser(ID);

        // Check same values
        Assert.assertThat(actual, Matchers.samePropertyValuesAs(expected));

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).findById(ID);
    }

    /**
     * Check that the system allows to retrieve a specific user by email.
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve a specific user by email.")
    public void retrieveOneByEmail() throws EntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Retrieve actual value
        final ProjectUser actual = projectUserService.retrieveOneByEmail(EMAIL);

        // Check same values
        Assert.assertThat(actual, Matchers.samePropertyValuesAs(projectUser));

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).findOneByEmail(EMAIL);
    }

    /**
     * Check that the system fails when trying to retrieve a user with unknown email.
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when trying to retrieve a user with unknown email.")
    public void retrieveOneByEmailNotFound() throws EntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());

        // Trigger the exception
        projectUserService.retrieveOneByEmail(EMAIL);
    }

    /**
     * Check that the system allows to retrieve the current logged user.
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the current logged user.")
    public void retrieveCurrentUser() throws EntityNotFoundException {

        Mockito.when(authResolver.getUser()).thenReturn(EMAIL);

        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Retrieve actual value
        final ProjectUser actual = projectUserService.retrieveCurrentUser();

        // Check
        Assert.assertThat(actual, Matchers.is(Matchers.equalTo(projectUser)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).findOneByEmail(EMAIL);
    }

    /**
     * Check that the system allows to retrieve all access requests for a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all access requests for a project.")
    public void retrieveAccessRequestList() {
        // Populate all projects users (which can be access requests or not)
        final List<ProjectUser> accessRequests = new ArrayList<>();
        accessRequests.add(new ProjectUser(null, null, null, null));
        accessRequests.add(new ProjectUser(null, null, null, null));

        final Pageable pageable = PageRequest.of(0, 100);

        try (final Stream<ProjectUser> stream = accessRequests.stream()) {
            // Prepare the list of expect values
            final List<ProjectUser> expected = stream.filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                    .collect(Collectors.toList());

            final Page<ProjectUser> expectedPage = new PageImpl<>(expected, pageable, 2);

            Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS, pageable))
                    .thenReturn(expectedPage);

            // Retrieve actual values
            final Page<ProjectUser> actual = projectUserService.retrieveAccessRequestList(pageable);

            // Lists must be equal
            Assert.assertEquals(expectedPage, actual);

            // Check that the repository's method was called with right arguments
            Mockito.verify(projectUserRepository).findByStatus(UserStatus.WAITING_ACCESS, pageable);

        }
    }

    /**
     * Check that the system fails when trying to update a non existing project user.
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when trying to update a non existing project user.")
    public void updateUserEntityNotFound() throws EntityException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.existsById(ID)).thenReturn(false);

        // Trigger the exception
        projectUserService.updateUser(ID, projectUser);
    }

    /**
     * Check that the system fails when user id differs from the passe id.
     */
    @Test(expected = EntityInconsistentIdentifierException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when user id differs from the passed id.")
    public void updateUserInvalidValue() throws EntityException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.existsById(ID)).thenReturn(true);

        // Trigger the exception
        projectUserService.updateUser(1L, projectUser);
    }

    /**
     * Check that the system allows to update a project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to update a project user.")
    public void updateUser() throws EntityException {
        // Mock repository
        Mockito.when(projectUserRepository.existsById(ID)).thenReturn(true);

        // Try to update a user
        projectUserService.updateUser(ID, projectUser);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).save(projectUser);
    }

    /**
     * Check that the system fails when trying to override a not exisiting user's access rights.
     * @throws EntityNotFoundException Thrown when no user of passed login could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Requirement("REGARDS_DSL_ADM_ADM_480")
    @Purpose("Check that the system fails when trying to override a not exisiting user's access rights.")
    public void updateUserAccessRightsEntityNotFound() throws EntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());

        // Trigger the exception
        projectUserService.updateUserAccessRights(EMAIL, new ArrayList<>());
    }

    /**
     * Check that the system allows to override role's access rights for a user.
     * @throws EntityNotFoundException Thrown when no user of passed login could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Requirement("REGARDS_DSL_ADM_ADM_480")
    @Purpose("Check that the system allows to override role's access rights for a user.")
    public void updateUserAccessRights() throws EntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Define updated permissions
        final List<ResourcesAccess> input = new ArrayList<>();
        // Updating an existing one
        final ResourcesAccess updatedPermission = new ResourcesAccess(0L, "updated desc0", "updated ms0",
                "updated res0", "Controller", RequestMethod.POST, DefaultRole.ADMIN);
        input.add(updatedPermission);
        // Adding a new permission
        final ResourcesAccess newPermission = new ResourcesAccess(2L, "desc2", "ms2", "res2", "Controller",
                RequestMethod.GET, DefaultRole.ADMIN);
        input.add(newPermission);

        // Define expected result
        final ProjectUser expected = new ProjectUser();
        expected.setId(ID);
        expected.setEmail(EMAIL);
        expected.setLastConnection(LAST_CONNECTION);
        expected.setLastUpdate(LAST_UPDATE);
        expected.setStatus(STATUS);
        expected.setMetadata(META_DATA);
        expected.setRole(ROLE);
        expected.setPermissions(new ArrayList<>());
        expected.getPermissions().add(updatedPermission);
        expected.getPermissions().add(newPermission);
        expected.getPermissions().add(projectUser.getPermissions().get(1));

        // Call method
        projectUserService.updateUserAccessRights(EMAIL, input);

        // Check
        Mockito.verify(projectUserRepository).save(Mockito.refEq(expected, "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system fail when trying to retrieve a user's permissions using a role not hierarchically inferior.
     * @throws EntityException various exceptions
     */
    @Test(expected = EntityOperationForbiddenException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system fail when trying to retrieve "
            + "a user's permissions using a role not hierarchically inferior.")
    public void retrieveProjectUserAccessRightsBorrowedRoleNotInferior() throws EntityException {
        // Define borrowed role
        final String borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        final Role borrowedRole = new Role();

        // Mock the repository
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        Mockito.when(roleService.retrieveRole(borrowedRoleName)).thenReturn(borrowedRole);
        // Make sure the borrowed role is not hierarchically inferior
        Mockito.when(roleService.isHierarchicallyInferior(borrowedRole, projectUser.getRole())).thenReturn(false);

        // Trigger the exception
        projectUserService.retrieveProjectUserAccessRights(EMAIL, borrowedRoleName);
    }

    /**
     * Check that the system allows to retrieve all permissions a of user using a borrowed role.
     * @throws EntityException various exceptions
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system allows to retrieve all permissions a of user using a borrowed role.")
    public void retrieveProjectUserAccessRightsWithBorrowedRole() throws EntityException {
        // Define borrowed role
        final Long borrowedRoleId = 99L;
        final String borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        final Set<ResourcesAccess> borrowedRolePermissions = new HashSet<>();
        borrowedRolePermissions.add(new ResourcesAccess(11L));
        borrowedRolePermissions.add(new ResourcesAccess(10L));
        borrowedRolePermissions.add(new ResourcesAccess(14L));
        final Role borrowedRole = new Role();
        borrowedRole.setId(borrowedRoleId);
        borrowedRole.setName(borrowedRoleName);
        borrowedRole.setPermissions(borrowedRolePermissions);

        // Mock the repository
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        Mockito.when(roleService.retrieveRole(borrowedRoleName)).thenReturn(borrowedRole);
        Mockito.when(roleService.retrieveRoleResourcesAccesses(borrowedRoleId)).thenReturn(borrowedRolePermissions);

        // Make sure the borrowed role is hierarchically inferior
        Mockito.when(roleService.isHierarchicallyInferior(borrowedRole, projectUser.getRole())).thenReturn(true);

        // Define expected permissions
        final List<ResourcesAccess> expected = new ArrayList<>();
        expected.addAll(borrowedRolePermissions);
        expected.addAll(projectUser.getPermissions());

        // Define actual result
        final List<ResourcesAccess> actual = projectUserService.retrieveProjectUserAccessRights(EMAIL,
                                                                                                borrowedRoleName);

        // Check
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertTrue(expected.containsAll(actual));
    }

    /**
     * Check that the system allows to retrieve all permissions a of user.
     * @throws EntityException various exceptions
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system allows to retrieve all permissions a of user.")
    public void retrieveProjectUserAccessRights() throws EntityException {
        // Define the user's role permissions
        final Set<ResourcesAccess> permissions = new HashSet<>();
        permissions.add(new ResourcesAccess(11L));
        permissions.add(new ResourcesAccess(10L));
        permissions.add(new ResourcesAccess(14L));
        projectUser.getRole().setPermissions(permissions);
        projectUser.getRole().setPermissions(permissions);
        projectUser.getRole().setPermissions(permissions);

        // Mock the repository
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        Mockito.when(roleService.retrieveRoleResourcesAccesses(projectUser.getRole().getId())).thenReturn(permissions);

        // Define expected permissions
        final List<ResourcesAccess> expected = new ArrayList<>();
        expected.addAll(permissions);
        expected.addAll(projectUser.getPermissions());

        // Define actual result
        final List<ResourcesAccess> actual = projectUserService.retrieveProjectUserAccessRights(EMAIL, null);

        // Check
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertTrue(expected.containsAll(actual));
    }

}
