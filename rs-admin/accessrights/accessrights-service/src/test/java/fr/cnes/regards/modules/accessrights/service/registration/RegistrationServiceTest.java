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
package fr.cnes.regards.modules.accessrights.service.registration;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners.WaitForQualificationListener;
import fr.cnes.regards.modules.accessrights.service.utils.AccountUtilsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

/**
 * Test class for {@link RegistrationService}.
 *
 * @author Xavier-Alexandre Brochard
 */
@RunWith(MockitoJUnitRunner.class)
public class RegistrationServiceTest {

    private static final String EMAIL = "email@test.com";

    private static final String FIRST_NAME = "Firstname";

    private static final String LAST_NAME = "Lirstname";

    private static final Set<MetaData> META_DATA = new HashSet<>();

    private static final String PASSWORD = "password";

    private static final List<ResourcesAccess> PERMISSIONS = new ArrayList<>();

    private static final Role ROLE = new Role("role name", null);

    private static final String ORIGIN_URL = "originUrl";

    private static final String REQUEST_LINK = "requestLink";

    private static final String ORIGIN = "origin";

    private static final Set<String> ACCESS_GROUPS = Collections.singleton("group");

    @Mock
    private IProjectUserService projectUserService;

    @Mock
    private IEmailVerificationTokenService tokenService;

    @Mock
    private WaitForQualificationListener listener;

    @Mock
    private AccountUtilsService accountUtilsService;

    @InjectMocks
    private RegistrationService registrationService;

    private AccessRequestDto accessRequestDto;

    private ProjectUser expectedProjectUser;

    private Account account;

    @Before
    public void setUp() throws EntityException {

        accessRequestDto = new AccessRequestDto(EMAIL,
                                                FIRST_NAME,
                                                LAST_NAME,
                                                ROLE.getName(),
                                                new ArrayList<>(META_DATA),
                                                PASSWORD,
                                                ORIGIN_URL,
                                                REQUEST_LINK,
                                                ORIGIN,
                                                ACCESS_GROUPS,
                                                0L);
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        expectedProjectUser = new ProjectUser().setEmail(EMAIL)
                                               .setLastName(LAST_NAME)
                                               .setFirstName(FIRST_NAME)
                                               .setPermissions(PERMISSIONS)
                                               .setRole(ROLE)
                                               .setMetadata(META_DATA)
                                               .setAccessGroups(ACCESS_GROUPS)
                                               .setStatus(UserStatus.WAITING_ACCOUNT_ACTIVE);

        Mockito.when(accountUtilsService.retrieveAccount(EMAIL)).thenReturn(account);
        Mockito.when(projectUserService.create(accessRequestDto, false, null, null))
               .thenReturn(expectedProjectUser.setId(69L));
        Mockito.when(projectUserService.create(accessRequestDto, true, null, null))
               .thenReturn(expectedProjectUser.setStatus(UserStatus.ACCESS_GRANTED).setOrigin(ORIGIN).setId(69L));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system fails when receiving an access request with an already used email.")
    public void requestAccessEmailAlreadyInUse() throws EntityException {
        // Given
        Mockito.when(projectUserService.create(accessRequestDto, false, null, null))
               .thenThrow(new EntityAlreadyExistsException("Nope"));
        // When - Then
        assertThrows(EntityAlreadyExistsException.class,
                     () -> registrationService.requestAccess(accessRequestDto, false));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration by creating a new project user.")
    public void requestAccess() throws EntityException {
        // Given
        account.setStatus(AccountStatus.ACTIVE);
        // When
        ProjectUser createdProjectUser = registrationService.requestAccess(accessRequestDto, false);
        // Then
        assertThat(createdProjectUser).usingRecursiveComparison()
                                      .ignoringFields("id", "lastConnection", "lastUpdate")
                                      .isEqualTo(expectedProjectUser);
        verify(tokenService).create(createdProjectUser, ORIGIN_URL, REQUEST_LINK);
        verify(listener).onAccountActivation(EMAIL);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose(
        "Check that the system allows the user to request a registration by creating a new project user for external accounts.")
    public void requestExternalAccess() throws EntityException {
        // When
        ProjectUser createdProjectUser = registrationService.requestAccess(accessRequestDto, true);
        // Then
        expectedProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
        expectedProjectUser.setOrigin(ORIGIN);
        assertThat(createdProjectUser).usingRecursiveComparison()
                                      .ignoringFields("id", "lastConnection", "lastUpdate")
                                      .isEqualTo(expectedProjectUser);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose(
        "Check that the system allows the user to request a registration by creating a new project user and account for external accounts.")
    public void requestExternalAccessWithAccountCreation() throws EntityException {
        // When
        ProjectUser createdProjectUser = registrationService.requestAccess(accessRequestDto, true);
        // Then
        expectedProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
        expectedProjectUser.setOrigin(ORIGIN);
        assertThat(createdProjectUser).usingRecursiveComparison()
                                      .ignoringFields("id", "lastConnection", "lastUpdate")
                                      .isEqualTo(expectedProjectUser);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system creates an Account when requesting an access if none already exists.")
    public void requestAccessNoAccount() throws EntityException {
        // When
        ProjectUser createdProjectUser = registrationService.requestAccess(accessRequestDto, false);
        // Then
        assertThat(createdProjectUser).usingRecursiveComparison()
                                      .ignoringFields("id", "lastConnection", "lastUpdate")
                                      .isEqualTo(expectedProjectUser);
    }

}
