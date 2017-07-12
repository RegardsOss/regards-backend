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
package fr.cnes.regards.modules.accessrights.service.account.workflow.state;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
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
import fr.cnes.regards.modules.accessrights.service.account.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

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

    private IEmailVerificationTokenService emailVerificationTokenService;

    private IAccountUnlockTokenService accountUnlockTokenService;

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
        emailVerificationTokenService = Mockito.mock(IEmailVerificationTokenService.class);
        accountUnlockTokenService = Mockito.mock(IAccountUnlockTokenService.class);

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
        Mockito.when(tenantResolver.getAllActiveTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(true);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService));

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
        Mockito.when(tenantResolver.getAllActiveTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService));

        // Call the method
        accountWorkflowManager.deleteAccount(account);

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).delete(ID);
        Mockito.verify(passwordResetService).deletePasswordResetTokenForAccount(account);
        Mockito.verify(accountUnlockTokenService).deleteAllByAccount(account);
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
        Mockito.when(tenantResolver.getAllActiveTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(true);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService));

        // Call the method
        final boolean result = accountWorkflowManager.canDelete(account);

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
        Mockito.when(tenantResolver.getAllActiveTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService));

        // Call the method
        final boolean result = accountWorkflowManager.canDelete(account);

        // Verify the repository was correctly called
        Assert.assertTrue(result);
    }

}
