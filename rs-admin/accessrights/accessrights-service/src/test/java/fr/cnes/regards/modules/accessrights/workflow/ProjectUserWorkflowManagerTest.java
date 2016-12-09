/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.AccessDeniedState;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.AccessGrantedState;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.ProjectUserStateProvider;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.WaitingAccessState;

/**
 * Test class for {@link ProjectUserWorkflowManager}.
 *
 * @author Xavier-Alexandre Brochard
 */
/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class ProjectUserWorkflowManagerTest {

    /**
     * Stub constant value for an email
     */
    private static final String EMAIL = "email@test.com";

    /**
     * Stub constant value for a first name
     */
    private static final String FIRST_NAME = "Firstname";

    /**
     * Stub constant value for a last name
     */
    private static final String LAST_NAME = "Lirstname";

    /**
     * Stub constant value for a lsit of meta data
     */
    private static final List<MetaData> META_DATA = new ArrayList<>();

    /**
     * Stub constant value for a password
     */
    private static final String PASSOWRD = "password";

    /**
     * Stub constant value for a list of permissions
     */
    private static final List<ResourcesAccess> PERMISSIONS = new ArrayList<>();

    /**
     * Stub constant value for a role
     */
    private static final Role ROLE = new Role("role name", null);

    /**
     * Mock repository of tested service
     */
    private IProjectUserRepository projectUserRepository;

    /**
     * Workflow manager for project users. Tested service.
     */
    private ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * Mock role servvice
     */
    private IRoleService roleService;

    /**
     * Mock account servvice
     */
    private IAccountService accountService;

    /**
     * The dto used to make an access request
     */
    private AccessRequestDto dto;

    /**
     * The project user which should be created by the dto
     */
    private ProjectUser projectUser;

    /**
     * Mocked project user state provider
     */
    private ProjectUserStateProvider projectUserStateProvider;

    /**
     * Mocked access settings service
     */
    private IAccessSettingsService accessSettingsService;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        roleService = Mockito.mock(IRoleService.class);
        accountService = Mockito.mock(IAccountService.class);
        projectUserStateProvider = Mockito.mock(ProjectUserStateProvider.class);
        accessSettingsService = Mockito.mock(IAccessSettingsService.class);

        // Create the tested service
        projectUserWorkflowManager = new ProjectUserWorkflowManager(projectUserStateProvider, projectUserRepository,
                roleService, accountService);

        // Prepare the access request
        dto = new AccessRequestDto();
        dto.setEmail(EMAIL);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setMetaData(META_DATA);
        dto.setPassword(PASSOWRD);
        dto.setPermissions(PERMISSIONS);
        dto.setRoleName(ROLE.getName());

        // Prepare the project user we expect to be created by the access request
        projectUser = new ProjectUser();
        projectUser.setEmail(EMAIL);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.setRole(ROLE);
        projectUser.setMetaData(META_DATA);
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
    }

    /**
     * Check that the system fails when receiving an access request with an already used email.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if the passed role culd not be found<br>
     *             {@link EntityAlreadyExistsException} Thrown if a {@link ProjectUser} with same <code>email</code>
     *             already exists<br>
     *             {@link EntityTransitionForbiddenException} when illegal transition call<br>
     */
    @Test(expected = EntityAlreadyExistsException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system fails when receiving an access request with an already used email.")
    public void requestAccessEmailAlreadyInUse() throws EntityException {
        // Prepare the duplicate
        final List<ProjectUser> projectUsers = new ArrayList<>();
        projectUsers.add(projectUser);
        Mockito.when(accountService.existAccount(EMAIL)).thenReturn(true);
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(new ProjectUser()));

        // Make sur they have the same email, in order to throw the expected exception
        Assert.assertTrue(projectUser.getEmail().equals(dto.getEmail()));

        // Trigger the exception
        projectUserWorkflowManager.requestProjectAccess(dto);
    }

    /**
     * Check that the system allows the user to request a registration.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if the passed role culd not be found<br>
     *             {@link EntityAlreadyExistsException} Thrown if a {@link ProjectUser} with same <code>email</code>
     *             already exists<br>
     *             {@link EntityTransitionForbiddenException} when illegal transition call<br>
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration by creating a new project user.")
    public void requestAccess() throws EntityException {
        // Mock
        Mockito.when(accountService.existAccount(EMAIL)).thenReturn(true);
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));
        Mockito.when(roleService.retrieveRole(projectUser.getRole().getName())).thenReturn(projectUser.getRole());

        // Call the service
        projectUserWorkflowManager.requestProjectAccess(dto);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status WAITING_ACCESS. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system set PUBLIC role as default when requesting access.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if the passed role culd not be found<br>
     *             {@link EntityAlreadyExistsException} Thrown if a {@link ProjectUser} with same <code>email</code>
     *             already exists<br>
     *             {@link EntityTransitionForbiddenException} when illegal transition call<br>
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system set PUBLIC role as default when requesting access.")
    public void requestAccessNullRoleName() throws EntityException {
        // Prepare the access request
        dto.setRoleName(null);

        // Mock
        final Role publicRole = new Role("Public", null);
        Mockito.when(roleService.getDefaultRole()).thenReturn(publicRole);
        Mockito.when(accountService.existAccount(EMAIL)).thenReturn(true);
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));

        // Prepare expected result
        projectUser.setRole(publicRole);

        // Call the service
        projectUserWorkflowManager.requestProjectAccess(dto);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status WAITING_ACCESS. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system creates an Account when requesting an access if none already exists.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if the passed role culd not be found<br>
     *             {@link EntityAlreadyExistsException} Thrown if a {@link ProjectUser} with same <code>email</code>
     *             already exists<br>
     *             {@link EntityTransitionForbiddenException} when illegal transition call<br>
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system creates an Account when requesting an access if none already exists.")
    public void requestAccessNoAccount() throws EntityException {
        // Make sure no account exists in order to make the service create a new one
        Mockito.when(accountService.existAccount(EMAIL)).thenReturn(false);

        // Trigger the exception
        projectUserWorkflowManager.requestProjectAccess(dto);
    }

    /**
     * Check that the system allows to delete a registration request.
     *
     * @throws EntityTransitionForbiddenException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void removeAccess() throws EntityTransitionForbiddenException {
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        final Pageable pageable = new PageRequest(0, 100);
        final Page<ProjectUser> expectedPage = new PageImpl<>(asList, pageable, 1);

        // Mock repository's content
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS, pageable)).thenReturn(expectedPage);
        Mockito.when(projectUserStateProvider.createState(projectUser))
                .thenReturn(new WaitingAccessState(projectUserRepository, accessSettingsService));

        // Call the tested method
        projectUserWorkflowManager.removeAccess(projectUser);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).delete(projectUser.getId());
    }

    /**
     * Check that the system allows to grant access to a previously access denied project user.
     *
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_DENIED
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to validate a registration request.")
    public void grantAccess() throws EntityTransitionForbiddenException {
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        final Pageable pageable = new PageRequest(0, 100);
        final Page<ProjectUser> expectedPage = new PageImpl<>(asList, pageable, 1);

        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserRepository.findByStatus(UserStatus.ACCESS_DENIED, pageable)).thenReturn(expectedPage);
        Mockito.when(projectUserStateProvider.createState(projectUser))
                .thenReturn(new AccessDeniedState(projectUserRepository));

        // Call the tested method
        projectUserWorkflowManager.grantAccess(projectUser);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care
        // about.
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system allows to deny a registration request.
     *
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_GRANTED
     *
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deny a registration request.")
    public void denyAccess() throws EntityTransitionForbiddenException {
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        final Pageable pageable = new PageRequest(0, 100);
        final Page<ProjectUser> expectedPage = new PageImpl<>(asList, pageable, 1);

        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserRepository.findByStatus(UserStatus.ACCESS_GRANTED, pageable)).thenReturn(expectedPage);
        Mockito.when(projectUserStateProvider.createState(projectUser))
                .thenReturn(new AccessGrantedState(projectUserRepository));

        // Call the tested method
        projectUserWorkflowManager.denyAccess(projectUser);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care
        // about.
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

}
