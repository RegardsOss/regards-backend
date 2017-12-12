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
package fr.cnes.regards.modules.accessrights.service.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.service.account.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.account.workflow.state.AccountStateProvider;
import fr.cnes.regards.modules.accessrights.service.account.workflow.state.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.account.workflow.state.PendingState;
import fr.cnes.regards.modules.accessrights.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Test class for {@link RegistrationService}.
 *
 * @author Xavier-Alexandre Brochard
 */
/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class RegistrationServiceTest {

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
     * Dummy origin url
     */
    private static final String ORIGIN_URL = "originUrl";

    /**
     * Dummy request link
     */
    private static final String REQUEST_LINK = "requestLink";

    /**
     * The tested service
     */
    private IRegistrationService registrationService;

    /**
     * Mock account repository
     */
    private IAccountRepository accountRepository;

    private IProjectUserRepository projectUserRepository;

    private IRoleService roleService;

    private IAccountSettingsService accountSettingsService;

    private AccessRequestDto dto;

    private ProjectUser projectUser;

    private Account account;

    private AccountSettings accountSettings;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        accountRepository = Mockito.mock(IAccountRepository.class);
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        roleService = Mockito.mock(IRoleService.class);
        IEmailVerificationTokenService tokenService = Mockito.mock(IEmailVerificationTokenService.class);
        accountSettingsService = Mockito.mock(IAccountSettingsService.class);
        AccountWorkflowManager accountWorkflowManager = Mockito.mock(AccountWorkflowManager.class);
        AccountStateProvider accountStateProvider = Mockito.mock(AccountStateProvider.class);
        IProjectUserService projectUserService = Mockito.mock(IProjectUserService.class);
        ITenantResolver tenantResolver = Mockito.mock(ITenantResolver.class);
        IRuntimeTenantResolver runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        IPasswordResetService passwordResetTokenService = Mockito.mock(IPasswordResetService.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        IAccountUnlockTokenService accountUnlockTokenService = Mockito.mock(IAccountUnlockTokenService.class);

        // Mock
        Mockito.when(roleService.getDefaultRole()).thenReturn(ROLE);
        Mockito.when(accountStateProvider.getState(account))
                .thenReturn(new PendingState(projectUserService, accountRepository, tenantResolver,
                        runtimeTenantResolver, passwordResetTokenService, tokenService, accountUnlockTokenService,
                        eventPublisher));

        // Create the tested service
        registrationService = new RegistrationService(projectUserRepository, roleService,
                                                      tokenService, eventPublisher, , );

        // Prepare the access request
        dto = new AccessRequestDto(EMAIL, FIRST_NAME, LAST_NAME, ROLE.getName(), META_DATA, PASSOWRD, ORIGIN_URL,
                REQUEST_LINK);

        // Prepare the account we expect to be create by the access request
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, EncryptionUtils.encryptPassword(PASSOWRD));

        // Prepare the project user we expect to be created by the access request
        projectUser = new ProjectUser();
        projectUser.setEmail(EMAIL);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.setRole(ROLE);
        projectUser.setMetadata(META_DATA);
        projectUser.setStatus(UserStatus.WAITING_ACCOUNT_ACTIVE);

        // Prepare account settings
        accountSettings = new AccountSettings();
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
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(account));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(new ProjectUser()));

        // Make sur they have the same email, in order to throw the expected exception
        Assert.assertTrue(projectUser.getEmail().equals(dto.getEmail()));

        // Trigger the exception
        registrationService.requestAccess(dto);
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
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(account));
        Mockito.when(accountSettingsService.retrieve()).thenReturn(accountSettings);
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));
        Mockito.when(roleService.retrieveRole(projectUser.getRole().getName())).thenReturn(projectUser.getRole());

        // Call the service
        registrationService.requestAccess(dto);

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
    @Test
    @SuppressWarnings("unchecked")
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system creates an Account when requesting an access if none already exists.")
    public void requestAccessNoAccount() throws EntityException {
        // Make sure no account exists in order to make the service create a new one.
        // The second call to the repository should then return the account, because it was creted
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(Optional.empty(), Optional.of(account));
        Mockito.when(accountSettingsService.retrieve()).thenReturn(accountSettings);
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));
        Mockito.when(roleService.retrieveRole(projectUser.getRole().getName())).thenReturn(projectUser.getRole());

        // Trigger the exception
        registrationService.requestAccess(dto);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(accountRepository).save(Mockito.refEq(account, "id", "passwordUpdateDate"));

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status WAITING_ACCOUNT_ACTIVE. We therefore exclude id, lastConnection and lastUpdate which we do not
        // care about
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
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
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Trigger the exception
        final AccessRequestDto dto = new AccessRequestDto(EMAIL, FIRST_NAME, LAST_NAME, ROLE.getName(), META_DATA,
                PASSOWRD, ORIGIN_URL, REQUEST_LINK);
        registrationService.requestAccess(dto);
    }

}
