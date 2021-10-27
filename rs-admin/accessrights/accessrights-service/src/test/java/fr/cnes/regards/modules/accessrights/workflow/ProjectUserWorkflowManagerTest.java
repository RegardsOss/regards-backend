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
package fr.cnes.regards.modules.accessrights.workflow;


import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.EmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AccessDeniedState;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserStateProvider;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.WaitingAccessState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;


/**
 * Test class for {@link ProjectUserWorkflowManager}.
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
    private static final Set<MetaData> META_DATA = new HashSet<>();

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

    private EmailVerificationTokenService tokenService;

    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IAccountsClient accountsClient;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        projectUserStateProvider = Mockito.mock(ProjectUserStateProvider.class);
        tokenService = Mockito.mock(EmailVerificationTokenService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        waitingAccessState = new WaitingAccessState(projectUserRepository, tokenService, Mockito.mock(IPublisher.class), accountsClient, runtimeTenantResolver, eventPublisher);

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
        Mockito.when(accountsClient.unlink(any(), any())).thenReturn(null);
        Mockito.doReturn("project").when(runtimeTenantResolver).getTenant();

        // Call the tested method
        projectUserWorkflowManager.removeAccess(projectUser);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).deleteById(projectUser.getId());
        Mockito.verify(accountsClient).unlink(projectUser.getEmail(), "project");
    }

    /**
     * Check that the system allows to grant access to a previously access denied project user.
     * @throws EntityException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to validate a registration request.")
    public void grantAccess() throws EntityException {
        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserStateProvider.createState(projectUser)).thenReturn(new AccessDeniedState(
                projectUserRepository, tokenService,  Mockito.mock(IPublisher.class), accountsClient, runtimeTenantResolver, eventPublisher));

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
        Mockito.when(projectUserStateProvider.createState(projectUser)).thenReturn(new WaitingAccessState(
                projectUserRepository, tokenService, Mockito.mock(IPublisher.class), accountsClient, runtimeTenantResolver, eventPublisher));

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
    @Purpose("Check that we send the verification email when granting access")
    public void grantAccess_shouldSendEmailIfProjectUserIsWaitingAccess() throws EntityException {
//        Account account = new Account(EMAIL, "First-Name", "Lastname", "password");
        EmailVerificationToken token = new EmailVerificationToken(projectUser, "originUrl", "requestLink");
        // Mock repository's content by making sure the request exists
        Mockito.when(projectUserStateProvider.createState(projectUser)).thenReturn(waitingAccessState);
        Mockito.when(tokenService.findByProjectUser(any())).thenReturn(token);
//        Mockito.when(accountService.retrieveAccountByEmail(EMAIL)).thenReturn(account);

        // Call the tested method
        projectUserWorkflowManager.grantAccess(projectUser);

        // Check that the AccountService#createAccount method was called to create an account containing values from the
        // DTO and with status PENDING. We therefore exclude id, lastConnection and lastUpdate which we do not care
        // about.
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        Mockito.verify(projectUserRepository).save(Mockito.refEq(projectUser, "id", "lastConnection", "lastUpdate"));
        Mockito.verify(eventPublisher).publishEvent(any(OnGrantAccessEvent.class));
    }

}
