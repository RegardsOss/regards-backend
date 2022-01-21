/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.service.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.ITenantService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

/**
 * Test class for {@link AccountWorkflowManager}.
 *
 * @author xbrochar
 */
public class AccountWorkflowManagerTest {

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
     * A dummy account
     */
    private static Account account;

    /**
     * Tested service
     */
    private AccountWorkflowManager accountWorkflowManager;

    /**
     * Mock repository
     */
    private IAccountRepository accountRepository;

    /**
     * Mocked tenant resolver
     */
    private ITenantService tenantService;

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

    private IAccountUnlockTokenService accountUnlockTokenService;

    private IProjectUsersClient projectUsersClient;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        // Mock dependencies
        projectUsersClient = Mockito.mock(IProjectUsersClient.class);
        accountRepository = Mockito.mock(IAccountRepository.class);
        tenantService = Mockito.mock(ITenantService.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        accountStateProvider = Mockito.mock(AccountStateProvider.class);
        passwordResetService = Mockito.mock(IPasswordResetService.class);
        accountUnlockTokenService = Mockito.mock(IAccountUnlockTokenService.class);

        // Mock authentication
        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        jwtAuth.setUser(new UserDetails(null, EMAIL, EMAIL, null));
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
        account.setProjects(Collections.singleton(new Project()));

        // Mock
        Mockito.when(accountRepository.findById(ID)).thenReturn(Optional.of(account));
        Mockito.when(tenantService.getAllActiveTenants(IProjectUsersClient.TARGET_NAME)).thenReturn(TENANTS);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(EMAIL))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(new ProjectUser()), HttpStatus.OK));
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUsersClient, accountRepository, tenantService, runtimeTenantResolver,
                        passwordResetService, accountUnlockTokenService));

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
        Mockito.when(accountRepository.findById(ID)).thenReturn(Optional.of(account));
        Mockito.when(tenantService.getAllActiveTenants(IProjectUsersClient.TARGET_NAME)).thenReturn(TENANTS);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(EMAIL))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUsersClient, accountRepository, tenantService, runtimeTenantResolver,
                        passwordResetService, accountUnlockTokenService));

        // Call the method
        accountWorkflowManager.deleteAccount(account);

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).deleteById(ID);
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
        account.setProjects(Collections.singleton(new Project()));

        // Mock
        Mockito.when(accountRepository.findById(ID)).thenReturn(Optional.of(account));
        Mockito.when(tenantService.getAllActiveTenants(IProjectUsersClient.TARGET_NAME)).thenReturn(TENANTS);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(EMAIL))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(new ProjectUser()), HttpStatus.OK));
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUsersClient, accountRepository, tenantService, runtimeTenantResolver,
                        passwordResetService, accountUnlockTokenService));

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
        account.setProjects(null);

        // Mock
        Mockito.when(accountRepository.findById(ID)).thenReturn(Optional.of(account));
        Mockito.when(tenantService.getAllActiveTenants(IProjectUsersClient.TARGET_NAME)).thenReturn(TENANTS);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(EMAIL))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new ActiveState(projectUsersClient, accountRepository, tenantService, runtimeTenantResolver,
                        passwordResetService, accountUnlockTokenService));

        // Call the method
        final boolean result = accountWorkflowManager.canDelete(account);

        // Verify the repository was correctly called
        Assert.assertTrue(result);
    }

}
