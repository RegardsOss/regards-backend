/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.AccessRequestService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Test class for {@link AccessRequestService}.
 *
 * @author CS SI
 */
public class AccessRequestServiceTest {

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
    private static final Role ROLE = new Role(0L, "role name", null, new ArrayList<>(), new ArrayList<>());

    /**
     * The tested service
     */
    private AccessRequestService accessRequestService;

    /**
     * Mock repository of tested service
     */
    private IProjectUserRepository projectUserRepository;

    /**
     * Mock account servvice
     */
    private IAccountService accountService;

    /**
     * Mock role servvice
     */
    private IRoleService roleService;

    /**
     * The dto used to make an access request
     */
    private AccessRequestDTO dto;

    /**
     * The project user which should be created by the dto
     */
    private ProjectUser projectUser;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        accountService = Mockito.mock(IAccountService.class);
        roleService = Mockito.mock(IRoleService.class);

        accessRequestService = new AccessRequestService(projectUserRepository, accountService, roleService);

        // Prepare the access request
        dto = new AccessRequestDTO();
        dto.setEmail(EMAIL);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setMetaData(META_DATA);
        dto.setPassword(PASSOWRD);
        dto.setPermissions(PERMISSIONS);
        dto.setRole(ROLE);

        // Prepare the project user we expect to be created by the access request
        projectUser = new ProjectUser();
        projectUser.setEmail(EMAIL);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.setRole(ROLE);
        projectUser.setMetaData(META_DATA);
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
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
            final List<ProjectUser> actual = accessRequestService.retrieveAccessRequestList();

            // Lists must be equal
            Assert.assertEquals(expected, actual);

            // Check that the repository's method was called with right arguments
            Mockito.verify(projectUserRepository).findByStatus(UserStatus.WAITING_ACCESS);

        }
    }

    /**
     * Check that the system fails when receiving a duplicate access request.
     *
     * @throws AlreadyExistingException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test(expected = AlreadyExistingException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system fails when receiving a duplicate access request.")
    public void requestAccessAlreadyExisting() throws AlreadyExistingException {
        // Prepare the duplicate
        final List<ProjectUser> projectUsers = new ArrayList<>();
        projectUsers.add(projectUser);
        // projectUsers.add(new ProjectUser(0L, null, null, UserStatus.WAITING_ACCESS, null, null, null, EMAIL));
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(projectUsers);

        // Make sur they have the same email, in order to throw the expected exception
        Assert.assertTrue(projectUser.getEmail().equals(dto.getEmail()));

        // Trigger the exception
        accessRequestService.requestAccess(dto);
    }

    /**
     * Check that the system allows the user to request a registration.
     *
     * @throws AlreadyExistingException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration by creating a new project user.")
    public void requestAccess() throws AlreadyExistingException {
        // Call the service
        accessRequestService.requestAccess(dto);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status WAITING_ACCESS. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system set PUBLIC role as default when requesting access.
     *
     * @throws AlreadyExistingException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system set PUBLIC role as default when requesting access.")
    public void requestAccessNullRole() throws AlreadyExistingException {
        // Prepare the access request
        dto.setRole(null);

        // Mock role repository
        final Role publicRole = new Role(0L, "Public", null, null, null);
        Mockito.when(roleService.getDefaultRole()).thenReturn(publicRole);

        // Prepare expected result
        projectUser.setRole(publicRole);

        // Call the service
        accessRequestService.requestAccess(dto);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status WAITING_ACCESS. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system creates an Account when requesting an access if none already exists.
     *
     * @throws AlreadyExistingException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system creates an Account when requesting an access if none already exists.")
    public void requestAccessNoAccount() throws AlreadyExistingException {
        // Make sure no account exists in order to make the service create a new one
        Mockito.when(accountService.existAccount(EMAIL)).thenReturn(false);

        // Prepare the account we exepect to be created
        // final Account account = new Account();
        final Account account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSOWRD);

        // Call the service
        accessRequestService.requestAccess(dto);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id and code which we do not care about.
        Mockito.verify(accountService).createAccount(Mockito.refEq(account, "id", "code"));
    }

    /**
     * Check that the system fails when trying to delete an inexistent registration request.
     *
     * @throws EntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system fails when trying to delete an inexistent registration request.")
    public void removeAccessRequestNotFound() throws EntityNotFoundException {
        final Long id = 0L;

        // Mock repository's content
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(new ArrayList<>());

        // Trigger the exception
        accessRequestService.removeAccessRequest(id);
    }

    /**
     * Check that the system allows to delete a registration request.
     *
     * @throws EntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void removeAccessRequest() throws EntityNotFoundException {
        final Long id = 0L;
        projectUser.setId(id);
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        // Mock repository's content
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(asList);

        // Call the tested method
        accessRequestService.removeAccessRequest(id);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).delete(id);
    }

    /**
     * Check that the system fails when trying to accept an inexistent registration request.
     *
     * @throws EntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system fails when trying to accept an inexistent registration request.")
    public void acceptAccessRequestNotFound() throws EntityNotFoundException {
        final Long id = 0L;

        // Mock repository's content
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(new ArrayList<>());

        // Trigger the exception
        accessRequestService.acceptAccessRequest(id);
    }

    /**
     * Check that the system allows to validate a registration request.
     *
     * @throws EntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to validate a registration request.")
    public void acceptAccessRequest() throws EntityNotFoundException {
        final Long id = 0L;
        projectUser.setId(id);
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(asList);

        // Call the tested method
        accessRequestService.acceptAccessRequest(id);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care
        // about.
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    /**
     * Check that the system fails when trying to deny a inexistent registration request.
     *
     * @throws EntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system fails when trying to deny a inexistent registration request.")
    public void denyAccessRequestNotFound() throws EntityNotFoundException {
        final Long id = 0L;

        // Mock repository's content
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(new ArrayList<>());

        // Trigger the exception
        accessRequestService.denyAccessRequest(id);
    }

    /**
     * Check that the system allows to deny a registration request.
     *
     * @throws EntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deny a registration request.")
    public void denyAccessRequest() throws EntityNotFoundException {
        final Long id = 0L;
        projectUser.setId(id);
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(asList);

        // Call the tested method
        accessRequestService.denyAccessRequest(id);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care
        // about.
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
    }

    @Test
    public void existsByEmailTrue() {
        final Long id = 0L;
        projectUser.setId(id);
        final List<ProjectUser> asList = new ArrayList<>();
        asList.add(projectUser);

        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(asList);

        // Check result
        Assert.assertTrue(accessRequestService.exists(id));
    }

    @Test
    public void existsByEmailFalse() {
        final Long id = 0L;

        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS)).thenReturn(new ArrayList<>());

        // Check result
        Assert.assertFalse(accessRequestService.exists(id));
    }
}
