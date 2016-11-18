/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Test class for {@link ProjectUserService}.
 *
 * @author xbrochar
 */
public class ProjectUserServiceTest {

    /**
     * A sample project user
     */
    private static ProjectUser projectUser = new ProjectUser();

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
    private static final LocalDateTime LAST_CONNECTION = LocalDateTime.now().minusDays(2);

    /**
     * A sample last update date
     */
    private static final LocalDateTime LAST_UPDATE = LocalDateTime.now().minusHours(1);

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
     * The tested service
     */
    private IProjectUserService projectUserService;

    /**
     * The project users workflow manager
     */
    private ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * Mocked CRUD repository managing {@link ProjectUser}s
     */
    private IProjectUserRepository projectUserRepository;

    /**
     * Mocked service handling CRUD operation on {@link Role}s
     */
    private IRoleService roleService;

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
        projectUser.setMetaData(META_DATA);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.getPermissions().add(new ResourcesAccess(0L, "desc0", "ms0", "res0", HttpVerb.GET));
        projectUser.getPermissions().add(new ResourcesAccess(1L, "desc1", "ms1", "res1", HttpVerb.PUT));
        projectUser.setRole(ROLE);

        // Mock untested services & repos
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        roleService = Mockito.mock(IRoleService.class);

        // Construct the tested service
        projectUserService = new ProjectUserService(projectUserRepository, roleService, "instance_admin@regards.fr",
                projectUserWorkflowManager);
    }

    /**
     * Check that the system allows to retrieve the users of a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the users of a project.")
    public void retrieveUserList() {
        // Define expected
        final List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);

        // Mock the repository returned value
        Mockito.when(projectUserRepository.findByStatus(UserStatus.ACCESS_GRANTED)).thenReturn(expected);

        // Retrieve actual value
        final List<ProjectUser> actual = projectUserService.retrieveUserList();

        // Check that the expected and actual role have same values
        Assert.assertEquals(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).findByStatus(UserStatus.ACCESS_GRANTED);
    }

    /**
     * Check that the system allows to retrieve a specific user without exposing hidden meta data.
     *
     * @throws EntityNotFoundException
     *             When no user with passed id could be found
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
        projectUser.getMetaData().add(metaData0);
        final MetaData metaData1 = new MetaData();
        metaData1.setVisibility(UserVisibility.READABLE);
        projectUser.getMetaData().add(metaData1);
        final MetaData metaData2 = new MetaData();
        metaData2.setVisibility(UserVisibility.WRITEABLE);
        projectUser.getMetaData().add(metaData2);

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
        expected.setMetaData(visibleMetaData);

        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOne(ID)).thenReturn(projectUser);

        // Retrieve actual value
        final ProjectUser actual = projectUserService.retrieveUser(ID);

        // Check same values
        Assert.assertThat(actual, Matchers.samePropertyValuesAs(expected));

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).findOne(ID);
    }

    /**
     * Check that the system allows to retrieve a specific user by email.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve a specific user by email.")
    public void retrieveOneByEmail() throws ModuleEntityNotFoundException {
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
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test(expected = ModuleEntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when trying to retrieve a user with unknown email.")
    public void retrieveOneByEmailNotFound() throws ModuleEntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());

        // Trigger the exception
        projectUserService.retrieveOneByEmail(EMAIL);
    }

    /**
     * Check that the system allows to retrieve the current logged user.
     *
     * @throws ModuleEntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to retrieve the current logged user.")
    public void retrieveCurrentUser() throws ModuleEntityNotFoundException {
        // Mock authentication
        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        final UserDetails details = new UserDetails();
        details.setName(EMAIL);
        jwtAuth.setUser(details);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

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
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(accessRequests);

        try (final Stream<ProjectUser> stream = accessRequests.stream()) {
            // Prepare the list of expect values
            final List<ProjectUser> expected = stream.filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                    .collect(Collectors.toList());

            // Retrieve actual values
            final List<ProjectUser> actual = projectUserService.retrieveAccessRequestList();

            // Lists must be equal
            Assert.assertEquals(expected, actual);

            // Check that the repository's method was called with right arguments
            Mockito.verify(projectUserRepository).findByStatus(UserStatus.WAITING_ACCESS);

        }
    }

    /**
     * Check that the system fails when trying to update a non existing project user.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     * @throws InvalidValueException
     *             Thrown when user id differs from the passed id
     */
    @Test(expected = ModuleEntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when trying to update a non existing project user.")
    public void updateUserEntityNotFound() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.exists(ID)).thenReturn(false);

        // Trigger the exception
        projectUserService.updateUser(ID, projectUser);
    }

    /**
     * Check that the system fails when user id differs from the passe id.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     * @throws InvalidValueException
     *             Thrown when user id differs from the passed id
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system fails when user id differs from the passed id.")
    public void updateUserInvalidValue() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.exists(ID)).thenReturn(true);

        // Trigger the exception
        projectUserService.updateUser(1L, projectUser);
    }

    /**
     * Check that the system allows to update a project user.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     * @throws InvalidValueException
     *             Thrown when user id differs from the passed id
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Requirement("REGARDS_DSL_ADM_ADM_320")
    @Purpose("Check that the system allows to update a project user.")
    public void updateUser() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock repository
        Mockito.when(projectUserRepository.exists(ID)).thenReturn(true);

        // Try to update a user
        projectUserService.updateUser(ID, projectUser);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).save(projectUser);
    }

    /**
     * Check that the system fails when trying to override a not exisiting user's access rights.
     *
     * @throws EntityNotFoundException
     *             Thrown when no user of passed login could be found
     */
    @Test(expected = ModuleEntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Requirement("REGARDS_DSL_ADM_ADM_480")
    @Purpose("Check that the system fails when trying to override a not exisiting user's access rights.")
    public void updateUserAccessRightsEntityNotFound() throws ModuleEntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty());

        // Trigger the exception
        projectUserService.updateUserAccessRights(EMAIL, new ArrayList<>());
    }

    /**
     * Check that the system allows to override role's access rights for a user.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no user of passed login could be found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Requirement("REGARDS_DSL_ADM_ADM_480")
    @Purpose("Check that the system allows to override role's access rights for a user.")
    public void updateUserAccessRights() throws ModuleEntityNotFoundException {
        // Mock the repository returned value
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Define updated permissions
        final List<ResourcesAccess> input = new ArrayList<>();
        // Updating an existing one
        final ResourcesAccess updatedPermission = new ResourcesAccess(0L, "updated desc0", "updated ms0",
                "updated res0", HttpVerb.POST);
        input.add(updatedPermission);
        // Adding a new permission
        final ResourcesAccess newPermission = new ResourcesAccess(2L, "desc2", "ms2", "res2", HttpVerb.GET);
        input.add(newPermission);

        // Define expected result
        final ProjectUser expected = new ProjectUser();
        expected.setId(ID);
        expected.setEmail(EMAIL);
        expected.setLastConnection(LAST_CONNECTION);
        expected.setLastUpdate(LAST_UPDATE);
        expected.setStatus(STATUS);
        expected.setMetaData(META_DATA);
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
     *
     * @throws InvalidValueException
     *             Thrown when the passed {@link Role} is not hierarchically inferior to the true {@link ProjectUser}'s
     *             <code>role</code>.
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system fail when trying to retrieve "
            + "a user's permissions using a role not hierarchically inferior.")
    public void retrieveProjectUserAccessRightsBorrowedRoleNotInferior()
            throws InvalidValueException, ModuleEntityNotFoundException {
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
     *
     * @throws InvalidValueException
     *             Thrown when the passed {@link Role} is not hierarchically inferior to the true {@link ProjectUser}'s
     *             <code>role</code>.
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system allows to retrieve all permissions a of user using a borrowed role.")
    public void retrieveProjectUserAccessRightsWithBorrowedRole()
            throws InvalidValueException, ModuleEntityNotFoundException {
        // Define borrowed role
        final Long borrowedRoleId = 99L;
        final String borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        final List<ResourcesAccess> borrowedRolePermissions = new ArrayList<>();
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
        Mockito.when(roleService.retrieveRoleResourcesAccessList(borrowedRoleId)).thenReturn(borrowedRolePermissions);

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
     *
     * @throws InvalidValueException
     *             Thrown when the passed {@link Role} is not hierarchically inferior to the true {@link ProjectUser}'s
     *             <code>role</code>.
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_260")
    @Purpose("Check that the system allows to retrieve all permissions a of user.")
    public void retrieveProjectUserAccessRights() throws InvalidValueException, ModuleEntityNotFoundException {
        // Define the user's role permissions
        final List<ResourcesAccess> permissions = new ArrayList<>();
        permissions.add(new ResourcesAccess(11L));
        permissions.add(new ResourcesAccess(10L));
        permissions.add(new ResourcesAccess(14L));
        projectUser.getRole().setPermissions(permissions);
        projectUser.getRole().setPermissions(permissions);
        projectUser.getRole().setPermissions(permissions);

        // Mock the repository
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));
        Mockito.when(roleService.retrieveRoleResourcesAccessList(projectUser.getRole().getId()))
                .thenReturn(permissions);

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
