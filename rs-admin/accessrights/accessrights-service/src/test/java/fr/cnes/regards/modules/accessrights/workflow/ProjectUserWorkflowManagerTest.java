/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.account.AccountService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.EmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AccessDeniedState;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AccessGrantedState;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AccessQualification;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserStateProvider;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.WaitingAccessState;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.TemplateService;

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
     * Stub constant value for a lsit of meta data
     */
    private static final List<MetaData> META_DATA = new ArrayList<>();

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
     * The project user which should be created by the dto
     */
    private ProjectUser projectUser;

    /**
     * Mocked project user state provider
     */
    private ProjectUserStateProvider projectUserStateProvider;

    /**
     * The waiting access state
     */
    private WaitingAccessState waitingAccessState;

    /**
     * The mocked email client
     */
    private IEmailClient emailClient;

    /**
     * Mocked account service
     */
    private IAccountService accountService;

    private EmailVerificationTokenService tokenService;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        projectUserStateProvider = Mockito.mock(ProjectUserStateProvider.class);

        tokenService = Mockito.mock(EmailVerificationTokenService.class);
        accountService = Mockito.mock(AccountService.class);
        TemplateService templateService = Mockito.mock(TemplateService.class);
        emailClient = Mockito.mock(IEmailClient.class);
        waitingAccessState = new WaitingAccessState(projectUserRepository, tokenService, accountService,
                templateService, emailClient);

        // Create the tested service
        projectUserWorkflowManager = new ProjectUserWorkflowManager(projectUserStateProvider);

        // Prepare the project user we expect to be created by the access request
        projectUser = new ProjectUser();
        projectUser.setEmail(EMAIL);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.setRole(ROLE);
        projectUser.setMetadata(META_DATA);
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
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
        // Mock repository's content
        Mockito.when(projectUserStateProvider.createState(projectUser)).thenReturn(waitingAccessState);

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
        // Mock repository's content by making sure the request exists
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
        // Mock repository's content by making sure the request exists
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

    /**
     * Check that we send the verification email when granting access
     *
     * @throws EntityException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("TODO")
    public void qualifyAccess() throws EntityException {
        Account account = new Account(EMAIL, "First-Name", "Lastname", "password");
        EmailVerificationToken token = new EmailVerificationToken(projectUser, "originUrl", "requestLink");
        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserStateProvider.createState(projectUser)).thenReturn(waitingAccessState);
        Mockito.when(tokenService.findByProjectUser(Mockito.any())).thenReturn(token);
        Mockito.when(accountService.retrieveAccountByEmail(EMAIL)).thenReturn(account);

        // Call the tested method
        projectUserWorkflowManager.qualifyAccess(projectUser, AccessQualification.GRANTED);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care
        // about.
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
        Mockito.verify(emailClient).sendEmail(Mockito.any());
    }

}
