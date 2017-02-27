/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountStateProvider;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.workflow.account.ActiveState;
import fr.cnes.regards.modules.accessrights.workflow.account.PendingState;

/**
 * Test class for {@link ProjectUserService}.
 *
 * @author xbrochar
 */
public class AccountWorkflowManagerTest {

    /**
     * A dummy account
     */
    private static Account account;

    /**
     * Dummy id
     */
    private static final Long ID = 0L;

    /**
     * Dummy email
     */
    private static final String EMAIL = "email@test.com";

    /**
     * Dummy first name
     */
    private static final String FIRST_NAME = "Firstname";

    /**
     * Dummy last name
     */
    private static final String LAST_NAME = "Lastname";

    /**
     * Dummy password
     */
    private static final String PASSWORD = "password";

    /**
     * Dummy unlock code
     */
    private static final String CODE = "code";

    /**
     * Dummy tenants
     */
    private static final Set<String> TENANTS = new HashSet<>(Arrays.asList("tenant0", "tenant1"));

    /**
     * Dummy account settings
     */
    private static AccountSettings accountSettings;

    /**
     * Tested service
     */
    private AccountWorkflowManager accountWorkflowManager;

    /**
     * Mock repository
     */
    private IAccountRepository accountRepository;

    /**
     * Mocked service managing {@link ProjectUser}s
     */
    private IProjectUserService projectUserService;

    /**
     * Mocked tenant resolver
     */
    private ITenantResolver tenantResolver;

    /**
     * Mocked runtime tenant resolver
     */
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Mocked account state factory
     */
    private AccountStateProvider accountStateProvider;

    /**
     * Mock Account Settings Service
     */
    private IAccountSettingsService accountSettingsService;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        accountSettings = new AccountSettings();
        // Mock dependencies
        accountRepository = Mockito.mock(IAccountRepository.class);
        projectUserService = Mockito.mock(IProjectUserService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        accountStateProvider = Mockito.mock(AccountStateProvider.class);
        accountSettingsService = Mockito.mock(IAccountSettingsService.class);

        // Mock authentication
        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        final UserDetails details = new UserDetails();
        details.setName(EMAIL);
        jwtAuth.setUser(details);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        // Construct the tested service with mock deps
        accountWorkflowManager = new AccountWorkflowManager(accountStateProvider, accountRepository,
                accountSettingsService);
    }

    /**
     * Check that the system prevents from deleting an account if it is still linked to project users.
     *
     * @throws ModuleException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test(expected = EntityOperationForbiddenException.class)
    @Purpose("Check that the system prevents from deleting an account if it is still linked to project users.")
    public void removeAccountNotDeletable() throws ModuleException {
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(true);
        Mockito.when(accountStateProvider.getState(account)).thenReturn(new ActiveState(projectUserService,
                accountRepository, tenantResolver, runtimeTenantResolver));

        // Trigger the exception
        accountWorkflowManager.deleteAccount(account);
    }

    /**
     * Check that the system prevents from deleting an account for certain status (ACCEPTED...).
     *
     * @throws ModuleException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test(expected = EntityTransitionForbiddenException.class)
    @Purpose("Check that the system prevents from deleting an account for certain status (ACCEPTED...).")
    public void removeAccountWrongStatus() throws ModuleException {
        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateProvider.getState(account)).thenReturn(new ActiveState(projectUserService,
                accountRepository, tenantResolver, runtimeTenantResolver));
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACCEPTED);

        // Trigger the exception
        accountWorkflowManager.deleteAccount(account);
    }

    /**
     * Check that the system allows to delete an account.
     *
     * @throws ModuleException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test
    @Purpose("Check that the system allows to delete an account.")
    public void removeAccount() throws ModuleException {
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateProvider.getState(account)).thenReturn(new ActiveState(projectUserService,
                accountRepository, tenantResolver, runtimeTenantResolver));

        // Call the method
        accountWorkflowManager.deleteAccount(account);

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).delete(ID);
    }

    /**
     * Check that the system fails when trying to create an account of already existing email.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} If the account is not in valid status <br>
     *             {@link EntityAlreadyExistsException} If an account with same email already exists<br>
     */
    @Test(expected = EntityAlreadyExistsException.class)
    @Purpose("Check that the system fails when trying to create an account of already existing email.")
    public void requestAccountEmailAlreadyUsed() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(account));

        // Trigger the exception
        final AccessRequestDto dto = new AccessRequestDto();
        dto.setEmail(EMAIL);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        accountWorkflowManager.requestAccount(dto, "");
    }

    /**
     * Check that the system allows to create a new account.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} Thrown when no template could be found to send the validation
     *             email<br>
     *             {@link EntityAlreadyExistsException} Thrown when an account with same email already exists<br>
     */
    @Test
    @Purpose("Check that the system allows to create a new account.")
    public void requestAccountManual() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));
        Mockito.when(accountSettingsService.retrieve()).thenReturn(accountSettings);
        accountSettings.setMode("manual");

        // Call tested method
        final AccessRequestDto dto = new AccessRequestDto();
        dto.setEmail(EMAIL);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setPassword(PASSWORD);
        accountWorkflowManager.requestAccount(dto, "");

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).save(Mockito.refEq(account, "id", CODE));
    }

    /**
     * Check that the system allows to create a new account and auto-accept it if configured so.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} Thrown when no template could be found to send the validation
     *             email<br>
     *             {@link EntityAlreadyExistsException} Thrown when an account with same email already exists<br>
     */
    @Ignore
    @Test
    @Purpose("Check that the system allows to create a new account and auto-accept it if configured so.")
    public void requestAccountAutoAccept() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));
        Mockito.when(accountSettingsService.retrieve()).thenReturn(accountSettings);
        accountSettings.setMode("auto-accept");
        Mockito.when(accountStateProvider.getState(account)).thenReturn(new PendingState(accountRepository));

        // final SimpleMailMessage validationEmail = new SimpleMailMessage();
        // validationEmail.setFrom("system@regards.fr");
        // validationEmail.setTo(EMAIL);
        // validationEmail.setText(TemplateTestConstants.CONTENT_TEMPLATED);
        // validationEmail.setSubject(TemplateTestConstants.SUBJECT);
        // Mockito.when(templateService.writeToEmail(Mockito.any(), Mockito.any(), Mockito.any()))
        // .thenReturn(validationEmail);

        // Call tested method
        final AccessRequestDto dto = new AccessRequestDto();
        dto.setEmail(EMAIL);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setPassword(PASSWORD);
        accountWorkflowManager.requestAccount(dto, "");

        // In auto-accept mode, we expect the account to be directly saved as accepted
        account.setStatus(AccountStatus.ACCEPTED);
        // Verify the repository was correctly called
        Mockito.verify(accountRepository, Mockito.times(2)).save(Mockito.refEq(account, "id", CODE));
        // Mockito.verify(emailService).sendEmail(validationEmail);
    }

}
