/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.*;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.projectuser.AccessSettingsService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserGroupService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailService;
import fr.cnes.regards.modules.accessrights.service.utils.AccountUtilsService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link ProjectUserService}.
 *
 * @author xbrochar
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectUserServiceTest {

    private static final Long ID = 0L;

    private static final String EMAIL = "user@email.com";

    private static final OffsetDateTime LAST_CONNECTION = OffsetDateTime.now().minusDays(2);

    private static final OffsetDateTime LAST_UPDATE = OffsetDateTime.now().minusHours(1);

    private static final UserStatus STATUS = UserStatus.ACCESS_GRANTED;

    private static final Set<MetaData> META_DATA = new HashSet<>();

    private static final Role ROLE = new Role(DefaultRole.ADMIN.toString(), null);

    private static final String LASTNAME = "lastName";

    private static final String FIRSTNAME = "firstName";

    private static final String ROLE_NAME = "roleName";

    private static final String PASSWORD = "password";

    private static final String DEFAULT_ROLE_NAME = DefaultRole.REGISTERED_USER.name();

    private static final ResourcesAccess PERMISSION_0 = new ResourcesAccess(0L,
                                                                            "desc0",
                                                                            "ms0",
                                                                            "res0",
                                                                            "Controller",
                                                                            RequestMethod.GET,
                                                                            DefaultRole.ADMIN);

    private static final ResourcesAccess PERMISSION_1 = new ResourcesAccess(1L,
                                                                            "desc1",
                                                                            "ms1",
                                                                            "res1",
                                                                            "Controller",
                                                                            RequestMethod.PUT,
                                                                            DefaultRole.ADMIN);

    private static final List<ResourcesAccess> PERMISSIONS = Arrays.asList(PERMISSION_0, PERMISSION_1);

    private ProjectUser projectUser;

    private AccessRequestDto accessRequest;

    private AccessRequestDto accessRequestFull;

    private Account account;

    @InjectMocks
    private ProjectUserService projectUserService;

    @Mock
    private IProjectUserRepository projectUserRepository;

    @Mock
    private IRoleService roleService;

    @Mock
    private IAuthenticationResolver authenticationResolver;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccessSettingsService accessSettingsService;

    @Mock
    private AccountUtilsService accountUtilsService;

    @Mock
    private AccessRightsEmailService accessRightsEmailService;

    @Mock
    private ProjectUserGroupService projectUserGroupService;

    @Mock
    private QuotaHelperService quotaHelperService;

    @Mock
    private IPublisher publisher;

    @Before
    public void init() throws EntityException {

        String accessGroup = "group69";
        ReflectionTestUtils.setField(projectUserService, "instanceAdminUserEmail", "admin@regards.com");
        when(projectUserRepository.save(any(ProjectUser.class))).thenAnswer(invocation -> ((ProjectUser) invocation.getArgument(
            0)).setId(ID));
        when(accessSettingsService.defaultRole()).thenReturn(DEFAULT_ROLE_NAME);
        when(accessSettingsService.defaultGroups()).thenReturn(Arrays.asList("group1", "group2", "groupDontKnowWhat"));
        when(accessSettingsService.userCreationMailRecipients()).thenReturn(Collections.singleton("admin@regards.fr"));
        when(roleService.retrieveRole(DEFAULT_ROLE_NAME)).thenReturn(new RoleFactory().doNotAutoCreateParents()
                                                                                      .createRegisteredUser());
        when(projectUserGroupService.getPublicGroups()).thenReturn(Collections.singleton("public"));
        when(quotaHelperService.getDefaultQuota()).thenReturn(42L);

        projectUser = new ProjectUser().setId(ID)
                                       .setEmail(EMAIL)
                                       .setFirstName(FIRSTNAME)
                                       .setLastName(LASTNAME)
                                       .setLastConnection(LAST_CONNECTION)
                                       .setLastUpdate(LAST_UPDATE)
                                       .setStatus(STATUS)
                                       .setMetadata(META_DATA)
                                       .setPermissions(PERMISSIONS)
                                       .setRole(ROLE);
        accessRequest = new AccessRequestDto().setEmail(EMAIL)
                                              .setFirstName(FIRSTNAME)
                                              .setLastName(LASTNAME)
                                              .setPassword("password")
                                              .setOriginUrl("url")
                                              .setRequestLink("link");
        accessRequestFull = new AccessRequestDto().setEmail(EMAIL)
                                                  .setFirstName(FIRSTNAME)
                                                  .setLastName(LASTNAME)
                                                  .setPassword("password")
                                                  .setOriginUrl("url")
                                                  .setRequestLink("link")
                                                  .setRoleName(ROLE_NAME)
                                                  .setAccessGroups(Collections.singleton(accessGroup));
        account = new Account(EMAIL, FIRSTNAME, LASTNAME, PASSWORD);
    }

    @Test
    @Purpose("Check that the system allows to create a new projectUser and the associated account.")
    public void createUserByBypassingRegistrationProcess() throws EntityException {

        // Given
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());
        when(projectUserRepository.save(any(ProjectUser.class))).thenAnswer(args -> args.getArgument(0,
                                                                                                     ProjectUser.class));
        when(accountUtilsService.retrieveAccount(EMAIL)).thenReturn(null);
        when(accountUtilsService.createAccount(accessRequest, false, AccountStatus.ACTIVE)).thenReturn(account);

        // When
        ProjectUser createdProjectUser = projectUserService.createProjectUser(accessRequest);

        // Then
        Assert.assertEquals(DEFAULT_ROLE_NAME, createdProjectUser.getRole().getName());
        Set<String> accessGroups = createdProjectUser.getAccessGroups();
        assertEquals(4, accessGroups.size());
        assertEquals(ProjectUser.REGARDS_ORIGIN, createdProjectUser.getOrigin());
        verify(accountUtilsService).createAccount(accessRequest, false, AccountStatus.ACTIVE);
        verify(projectUserRepository).save(any(ProjectUser.class));
        verify(accessRightsEmailService).sendEmail(any());
    }

    @Test
    @Purpose("Check that the system allows to create a new projectUser with the associated account.")
    public void createUserByBypassingRegistrationProcessWithoutAccount() throws EntityException {

        // Given
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());
        when(roleService.retrieveRole(ROLE_NAME)).thenReturn(new Role());
        when(projectUserRepository.save(any(ProjectUser.class))).thenAnswer(args -> args.getArgument(0,
                                                                                                     ProjectUser.class));
        when(accountUtilsService.retrieveAccount(EMAIL)).thenReturn(new Account(EMAIL, FIRSTNAME, LASTNAME, null));

        // When
        ProjectUser createdProjectUser = projectUserService.createProjectUser(accessRequestFull);

        // Then
        Set<String> accessGroups = createdProjectUser.getAccessGroups();
        assertEquals(5, accessGroups.size());
        assertTrue(accessGroups.contains("group69"));
        assertEquals(ProjectUser.REGARDS_ORIGIN, createdProjectUser.getOrigin());
        verify(accountUtilsService, Mockito.never()).createAccount(any(AccessRequestDto.class),
                                                                   any(boolean.class),
                                                                   any(AccountStatus.class));
        verify(projectUserRepository).save(any(ProjectUser.class));
        verify(accessRightsEmailService).sendEmail(any());
    }

    @Test
    @Purpose("Check that the system allows to create a new projectUser with the associated account.")
    public void createUserByBypassingRegistrationProcessError() {
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.of(new ProjectUser()));
        assertThrows(EntityAlreadyExistsException.class, () -> projectUserService.createProjectUser(accessRequest));
    }

    /**
     * Check that the system allows to retrieve the users of a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the users of a project.")
    public void retrieveUserListByStatus() {

        // Define expected
        List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);

        Pageable pageable = PageRequest.of(0, 100);
        Page<ProjectUser> expectedPage = new PageImpl<>(expected, pageable, 1);

        // Mock the repository returned value
        when(projectUserRepository.findAll(any(Specification.class), Mockito.eq(pageable))).thenReturn(expectedPage);

        // Retrieve actual value
        Page<ProjectUser> actual = projectUserService.retrieveUsers(new SearchProjectUserParameters().withStatusIncluded(
            Arrays.asList(UserStatus.ACCESS_GRANTED)), pageable);

        // Check that the expected and actual role have same values
        Assert.assertEquals(expectedPage, actual);
    }

    /**
     * Check that the system allows to retrieve the users of a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the users of a project.")
    public void retrieveAllUserList() {

        // Define expected
        List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);

        Pageable pageable = PageRequest.of(0, 100);
        Page<ProjectUser> expectedPage = new PageImpl<>(expected, pageable, 1);

        // Mock the repository returned value
        when(projectUserRepository.findAll((Specification<ProjectUser>) null, pageable)).thenReturn(expectedPage);

        // Retrieve actual value
        Page<ProjectUser> actual = projectUserService.retrieveUsers(null, pageable);

        Page<ProjectUser> actualProjectUsers = projectUserService.retrieveUsers(null, pageable);

        // Check that the expected and actual role have same values
        verify(projectUserRepository, times(2)).findAll((Specification<ProjectUser>) null, pageable);
        Assert.assertEquals(expectedPage, actual);
        Assert.assertEquals(expectedPage, actualProjectUsers);
    }

    /**
     * Check that the system allows to retrieve a specific user without exposing hidden meta data.
     *
     * @throws EntityNotFoundException When no user with passed id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve a specific user without exposing hidden meta data.")
    public void retrieveUser() throws EntityNotFoundException {

        // Define user as in db
        MetaData metaData0 = new MetaData("hidden", "", UserVisibility.HIDDEN);
        projectUser.getMetadata().add(metaData0);
        MetaData metaData1 = new MetaData("readable", "", UserVisibility.READABLE);
        projectUser.getMetadata().add(metaData1);
        MetaData metaData2 = new MetaData("writeable", "", UserVisibility.WRITEABLE);
        projectUser.getMetadata().add(metaData2);

        // Define user as expected
        Set<MetaData> visibleMetaData = new HashSet<>();
        visibleMetaData.add(metaData1);
        visibleMetaData.add(metaData2);
        ProjectUser expected = new ProjectUser().setId(ID)
                                                .setEmail(EMAIL)
                                                .setFirstName(FIRSTNAME)
                                                .setLastName(LASTNAME)
                                                .setLastUpdate(LAST_UPDATE)
                                                .setLastConnection(LAST_CONNECTION)
                                                .setStatus(STATUS)
                                                .setPermissions(PERMISSIONS)
                                                .setRole(ROLE)
                                                .setMetadata(visibleMetaData);

        // Mock the repository returned value
        when(projectUserRepository.findById(ID)).thenReturn(Optional.of(projectUser));

        // Retrieve actual value
        ProjectUser actual = projectUserService.retrieveUser(ID);

        // Check same values
        Assert.assertThat(actual, Matchers.samePropertyValuesAs(expected));

        // Check that the repository's method was called with right arguments
        verify(projectUserRepository).findById(ID);
    }

    /**
     * Check that the system allows to retrieve a specific user by email.
     *
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve a specific user by email.")
    public void retrieveOneByEmail() throws EntityNotFoundException {
        // Mock the repository returned value
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Retrieve actual value
        ProjectUser actual = projectUserService.retrieveOneByEmail(EMAIL);

        // Check same values
        Assert.assertThat(actual, Matchers.samePropertyValuesAs(projectUser));

        // Check that the repository's method was called with right arguments
        verify(projectUserRepository).findOneByEmail(EMAIL);
    }

    /**
     * Check that the system fails when trying to retrieve a user with unknown email.
     *
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when trying to retrieve a user with unknown email.")
    public void retrieveOneByEmailNotFound() throws EntityNotFoundException {
        // Mock the repository returned value
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());

        // Trigger the exception
        projectUserService.retrieveOneByEmail(EMAIL);
    }

    /**
     * Check that the system allows to retrieve the current logged user.
     *
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the current logged user.")
    public void retrieveCurrentUser() throws EntityNotFoundException {

        when(authenticationResolver.getUser()).thenReturn(EMAIL);

        // Mock the repository returned value
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Retrieve actual value
        ProjectUser actual = projectUserService.retrieveCurrentUser();

        // Check
        Assert.assertThat(actual, Matchers.is(Matchers.equalTo(projectUser)));

        // Check that the repository's method was called with right arguments
        verify(projectUserRepository).findOneByEmail(EMAIL);
    }

    /**
     * Check that the system allows to retrieve all access requests for a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all access requests for a project.")
    public void retrieveAccessRequestList() {

        // Populate all projects users (which can be access requests or not)
        List<ProjectUser> accessRequests = new ArrayList<>();
        accessRequests.add(new ProjectUser(null, null, null, null));
        accessRequests.add(new ProjectUser(null, null, null, null));

        Pageable pageable = PageRequest.of(0, 100);

        // Prepare the list of expected values
        List<ProjectUser> expected = accessRequests.stream()
                                                   .filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                                                   .collect(Collectors.toList());

        Page<ProjectUser> expectedPage = new PageImpl<>(expected, pageable, 2);

        when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS, pageable)).thenReturn(expectedPage);

        // Retrieve actual values
        Page<ProjectUser> actual = projectUserService.retrieveAccessRequestList(pageable);

        // Lists must be equal
        Assert.assertEquals(expectedPage, actual);

        // Check that the repository's method was called with right arguments
        verify(projectUserRepository).findByStatus(UserStatus.WAITING_ACCESS, pageable);
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
        when(projectUserRepository.findById(ID)).thenReturn(Optional.empty());

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
        when(projectUserRepository.findById(1L)).thenReturn(Optional.of(new ProjectUser().setId(1L)));
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
        when(projectUserRepository.findById(ID)).thenReturn(Optional.of(projectUser));

        // Try to update a user
        projectUserService.updateUser(ID, projectUser);

        // Check that the repository's method was called with right arguments
        verify(projectUserRepository).save(projectUser);
    }

    /**
     * Check that the system fails when trying to override a not exisiting user's access rights.
     *
     * @throws EntityNotFoundException Thrown when no user of passed login could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Requirement("REGARDS_DSL_ADM_ADM_480")
    @Purpose("Check that the system fails when trying to override a not exisiting user's access rights.")
    public void updateUserAccessRightsEntityNotFound() throws EntityNotFoundException {
        // Mock the repository returned value
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());

        // Trigger the exception
        projectUserService.updateUserAccessRights(EMAIL, new ArrayList<>());
    }

    /**
     * Check that the system allows to override role's access rights for a user.
     *
     * @throws EntityNotFoundException Thrown when no user of passed login could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Requirement("REGARDS_DSL_ADM_ADM_480")
    @Purpose("Check that the system allows to override role's access rights for a user.")
    public void updateUserAccessRights() throws EntityNotFoundException {
        // Mock the repository returned value
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Define updated permissions
        ResourcesAccess updatedPermission = new ResourcesAccess(0L,
                                                                "updated desc0",
                                                                "updated ms0",
                                                                "updated res0",
                                                                "Controller",
                                                                RequestMethod.POST,
                                                                DefaultRole.ADMIN);
        ResourcesAccess newPermission = new ResourcesAccess(2L,
                                                            "desc2",
                                                            "ms2",
                                                            "res2",
                                                            "Controller",
                                                            RequestMethod.GET,
                                                            DefaultRole.ADMIN);
        List<ResourcesAccess> input = Arrays.asList(updatedPermission, newPermission);

        // Define expected result
        ProjectUser expected = new ProjectUser();
        expected.setId(ID);
        expected.setEmail(EMAIL);
        expected.setFirstName(FIRSTNAME);
        expected.setLastName(LASTNAME);
        expected.setLastConnection(LAST_CONNECTION);
        expected.setLastUpdate(LAST_UPDATE);
        expected.setStatus(STATUS);
        expected.setMetadata(META_DATA);
        expected.setRole(ROLE);
        expected.setPermissions(Arrays.asList(updatedPermission, newPermission, PERMISSION_1));

        // Call method
        projectUserService.updateUserAccessRights(EMAIL, input);

        // Check
        verify(projectUserRepository).save(Mockito.refEq(expected, "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system fail when trying to retrieve a user's permissions using a role not hierarchically inferior.
     *
     * @throws EntityException various exceptions
     */
    @Test(expected = EntityOperationForbiddenException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system fail when trying to retrieve "
             + "a user's permissions using a role not hierarchically inferior.")
    public void retrieveProjectUserAccessRightsBorrowedRoleNotInferior() throws EntityException {
        // Define borrowed role
        String borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        Role borrowedRole = new Role();

        // Mock the repository
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        when(roleService.retrieveRole(borrowedRoleName)).thenReturn(borrowedRole);
        // Make sure the borrowed role is not hierarchically inferior
        when(roleService.isHierarchicallyInferiorOrEqual(borrowedRole, projectUser.getRole())).thenReturn(false);

        // Trigger the exception
        projectUserService.retrieveProjectUserAccessRights(EMAIL, borrowedRoleName);
    }

    /**
     * Check that the system allows to retrieve all permissions a of user using a borrowed role.
     *
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
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        when(roleService.retrieveRole(borrowedRoleName)).thenReturn(borrowedRole);
        when(roleService.retrieveRoleResourcesAccesses(borrowedRoleId)).thenReturn(borrowedRolePermissions);

        // Make sure the borrowed role is hierarchically inferior
        when(roleService.isHierarchicallyInferiorOrEqual(borrowedRole, projectUser.getRole())).thenReturn(true);

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
     *
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
        when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        when(roleService.retrieveRoleResourcesAccesses(projectUser.getRole().getId())).thenReturn(permissions);

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
