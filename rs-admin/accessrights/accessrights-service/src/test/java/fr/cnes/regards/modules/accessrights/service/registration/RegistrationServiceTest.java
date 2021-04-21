/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners.WaitForQualificationListener;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Test class for {@link RegistrationService}.
 *
 * @author Xavier-Alexandre Brochard
 */
@RunWith(MockitoJUnitRunner.class)
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

    @Mock
    private IProjectUserRepository projectUserRepository;

    @Mock
    private IRoleService roleService;

    @Mock
    private IAccountsClient accountsClient;

    @Mock
    private IEmailVerificationTokenService tokenService;

    @Mock
    private WaitForQualificationListener listener;

    @InjectMocks
    private RegistrationService registrationService;

    private AccessRequestDto dto;

    private ProjectUser projectUser;

    private Account account;

    @Before
    public void setUp() {

        Mockito.when(roleService.getDefaultRole()).thenReturn(ROLE);

        // Prepare the access request
        dto = new AccessRequestDto(EMAIL, FIRST_NAME, LAST_NAME, ROLE.getName(), META_DATA, PASSOWRD, ORIGIN_URL,
                                   REQUEST_LINK
        );

        // Prepare the account we expect to be create by the access request
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSOWRD);

        // Prepare the project user we expect to be created by the access request
        projectUser = new ProjectUser();
        projectUser.setEmail(EMAIL);
        projectUser.setPermissions(PERMISSIONS);
        projectUser.setRole(ROLE);
        projectUser.setMetadata(META_DATA);
        projectUser.setStatus(UserStatus.WAITING_ACCOUNT_ACTIVE);

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
        Mockito.when(accountsClient.retrieveAccounByEmail(dto.getEmail()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(account), HttpStatus.OK));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(new ProjectUser()));

        // Make sur they have the same email, in order to throw the expected exception
        Assert.assertEquals(projectUser.getEmail(), dto.getEmail());

        // Trigger the exception
        registrationService.requestAccess(dto, false);
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
        Mockito.when(accountsClient.retrieveAccounByEmail(dto.getEmail()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(account), HttpStatus.OK));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));

        // Call the service
        registrationService.requestAccess(dto, false);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status WAITING_ACCESS. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
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
    @Purpose("Check that the system allows the user to request a registration by creating a new project user for external accounts.")
    public void requestExternalAccess() throws EntityException {
        // Mock
        Mockito.when(accountsClient.retrieveAccounByEmail(dto.getEmail()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(account), HttpStatus.OK));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));

        // Call the service
        registrationService.requestAccess(dto, true);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status ACCESS_GRANTED. We therefore exclude id, lastConnection and lastUpdate which we do not care about.
        // With external accounts, the project user does not need to validate his email.
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
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
    @Purpose("Check that the system allows the user to request a registration by creating a new project user and account for external accounts.")
    public void requestExternalAccessWithAccountCreation() throws EntityException {
        Account newAccountToCreate = new Account(dto.getEmail(), dto.getFirstName(), dto.getLastName(), null);
        // Mock.
        // First call : Account not found
        // Second call : Return created account
        Answer<ResponseEntity<EntityModel<Account>>> accountsClientResponse = new Answer<ResponseEntity<EntityModel<Account>>>() {

            private int nbCalls = 0;

            @Override
            public ResponseEntity<EntityModel<Account>> answer(InvocationOnMock invocation) {
                nbCalls++;
                if (nbCalls == 1) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                } else {
                    return new ResponseEntity<>(new EntityModel<>(newAccountToCreate), HttpStatus.OK);
                }
            }

        };
        Mockito.when(accountsClient.retrieveAccounByEmail(dto.getEmail())).then(accountsClientResponse);
        Mockito.when(accountsClient.createAccount(Mockito.any()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(newAccountToCreate), HttpStatus.CREATED));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));

        // Call the service
        registrationService.requestAccess(dto, true);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status ACCESS_GRANTED. We therefore exclude id, lastConnection and lastUpdate which we do not care about.
        // With external accounts, the project user does not need to validate his email.
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
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
        Mockito.when(accountsClient.retrieveAccounByEmail(dto.getEmail()))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND),
                            new ResponseEntity<>(new EntityModel<>(account), HttpStatus.OK));
        AccountNPassword accountWithPassword = new AccountNPassword(account, account.getPassword());
        Mockito.when(accountsClient.createAccount(accountWithPassword))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(account), HttpStatus.CREATED));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(null));

        // Trigger the exception
        registrationService.requestAccess(dto, false);

        // Check that the repository's method was called to create a project user containing values from the DTO and
        // with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care about
        Mockito.verify(accountsClient).createAccount(Mockito.refEq(accountWithPassword, "id", "passwordUpdateDate"));

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
        Mockito.when(accountsClient.retrieveAccounByEmail(dto.getEmail()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(account), HttpStatus.OK));
        Mockito.when(projectUserRepository.findOneByEmail(EMAIL)).thenReturn(Optional.ofNullable(projectUser));

        // Trigger the exception
        final AccessRequestDto dto = new AccessRequestDto(EMAIL, FIRST_NAME, LAST_NAME, ROLE.getName(), META_DATA,
                PASSOWRD, ORIGIN_URL, REQUEST_LINK);
        registrationService.requestAccess(dto, false);
    }

}
