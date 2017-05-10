/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

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
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountStateProvider;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.workflow.account.ActiveState;

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
     * Dummy tenants
     */
    private static final Set<String> TENANTS = new HashSet<>(Arrays.asList("tenant0", "tenant1"));

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
     * Mocked service
     */
    private IPasswordResetService passwordResetService;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        // Mock dependencies
        accountRepository = Mockito.mock(IAccountRepository.class);
        projectUserService = Mockito.mock(IProjectUserService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        accountStateProvider = Mockito.mock(AccountStateProvider.class);
        passwordResetService = Mockito.mock(IPasswordResetService.class);

        // Mock authentication
        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        final UserDetails details = new UserDetails();
        details.setName(EMAIL);
        jwtAuth.setUser(details);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        // Construct the tested service with mock deps
        accountWorkflowManager = new AccountWorkflowManager(accountStateProvider);
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
                accountRepository, tenantResolver, runtimeTenantResolver, passwordResetService));

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
                accountRepository, tenantResolver, runtimeTenantResolver, passwordResetService));
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
                accountRepository, tenantResolver, runtimeTenantResolver, passwordResetService));

        // Call the method
        accountWorkflowManager.deleteAccount(account);

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).delete(ID);
    }

    /**
     * Check that the system does not delete an account linked to project users
     */
    @Test
    @Purpose("Check that the system does not delete an account in right status linked to project users.")
    public void canDelete_shouldReturnFalse() {
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(true);
        Mockito.when(accountStateProvider.getState(account)).thenReturn(new ActiveState(projectUserService,
                accountRepository, tenantResolver, runtimeTenantResolver, passwordResetService));

        // Call the method
        boolean result = accountWorkflowManager.canDelete(account);

        // Verify the repository was correctly called
        Assert.assertFalse(result);
    }

    /**
     * Check that the system allows to delete an account in right status if not linked to project users.
     */
    @Test
    @Purpose("Check that the system allows to delete an account in right status if not linked to project users.")
    public void canDelete_shouldReturnTrue() {
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateProvider.getState(account)).thenReturn(new ActiveState(projectUserService,
                accountRepository, tenantResolver, runtimeTenantResolver, passwordResetService));

        // Call the method
        boolean result = accountWorkflowManager.canDelete(account);

        // Verify the repository was correctly called
        Assert.assertTrue(result);
    }

}
