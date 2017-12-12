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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.account.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Test class for {@link ProjectUserService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class LockedStateTest {

    /**
     * A dummy account
     */
    private static Account account;

    /**
     * A dummy account unlock token
     */
    private static AccountUnlockToken accountUnlockToken;

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
     * Dummy token
     */
    private static final String TOKEN = "token";

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

    private IAccountService accountService;

    private IAccountUnlockTokenService accountUnlockTokenService;

    private ITemplateService templateService;

    private IEmailClient emailClient;

    /**
     * Mocked service
     */
    private IPasswordResetService passwordResetService;

    private IEmailVerificationTokenService emailVerificationTokenService;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        account.setId(ID);
        accountUnlockToken = new AccountUnlockToken(TOKEN, account);

        // Mock dependencies
        accountRepository = Mockito.mock(IAccountRepository.class);
        projectUserService = Mockito.mock(IProjectUserService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        accountStateProvider = Mockito.mock(AccountStateProvider.class);
        accountService = Mockito.mock(IAccountService.class);
        accountUnlockTokenService = Mockito.mock(IAccountUnlockTokenService.class);
        templateService = Mockito.mock(ITemplateService.class);
        emailClient = Mockito.mock(IEmailClient.class);
        passwordResetService = Mockito.mock(IPasswordResetService.class);
        emailVerificationTokenService = Mockito.mock(IEmailVerificationTokenService.class);

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
     * Check that the system does unlock not locked accounts and feedbacks the caller.
     *
     * @throws EntityException
     */
    @Test(expected = EntityTransitionForbiddenException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system does unlock not locked accounts and feedbacks the caller.")
    public void performUnlockAccountNotLocked() throws EntityException {
        // Prepare the error case
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService));
        Mockito.when(accountUnlockTokenService.findByToken(TOKEN)).thenReturn(accountUnlockToken);

        // Trigger exception
        accountWorkflowManager.performUnlockAccount(account, TOKEN);
    }

    /**
     * Check that the system does not unlock a locked account if the wrong code is passed.
     *
     * @throws EntityException
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system does not unlock a locked account if the wrong code is passed.")
    public void performUnlockAccountWrongCode() throws EntityException {
        // Prepare the case
        account.setStatus(AccountStatus.LOCKED);
        final String wrongToken = "wrongToken";

        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new LockedState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService, accountService, templateService, emailClient));
        Mockito.when(accountUnlockTokenService.findByToken(wrongToken))
                .thenThrow(new EntityNotFoundException(ID, AccountUnlockToken.class));

        // Trigger exception
        accountWorkflowManager.performUnlockAccount(account, wrongToken);
    }

    /**
     * Check that the system allows a user to unlock its account with a code.
     *
     * @throws EntityException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows a user to unlock its account with a code.")
    public void performUnlockAccount() throws EntityException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new LockedState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetService, emailVerificationTokenService,
                        accountUnlockTokenService, accountService, templateService, emailClient));
        Mockito.when(accountUnlockTokenService.findByToken(TOKEN)).thenReturn(accountUnlockToken);

        // Prepare the case
        account.setStatus(AccountStatus.LOCKED);

        // Call tested method
        accountWorkflowManager.performUnlockAccount(account, TOKEN);

        // Check
        account.setStatus(AccountStatus.ACTIVE);
        Mockito.verify(accountService).updateAccount(ID, account);
    }

}
