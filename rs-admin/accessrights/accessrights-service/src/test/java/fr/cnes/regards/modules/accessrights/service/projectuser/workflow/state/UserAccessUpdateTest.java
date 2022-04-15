/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnInactiveEvent;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.*;

public class UserAccessUpdateTest {

    protected static final String TENANT = "TENANT";

    protected final ProjectUserWorkflowManager userWorkflowManager;

    protected final IRuntimeTenantResolver currentTenantAccessor;

    protected final IAccountsClient accountAccessor;

    protected final IPublisher eventPublisher;

    protected final IEmailVerificationTokenService mailVerifier;

    protected final ApplicationEventPublisher platformPublisher;

    private final UserAccessUpdateFactory accessUpdateActionFactory;

    protected IProjectUserRepository userAccessor;

    public UserAccessUpdateTest() {
        userAccessor = mockUserAccessor();
        mailVerifier = mockMailVerifier();
        eventPublisher = mockEventPublisher();
        accountAccessor = mockAccountAccessor();
        currentTenantAccessor = mockTenant();
        platformPublisher = mockPlatformPublisher();
        accessUpdateActionFactory = new UserAccessUpdateFactory(userAccessor,
                                                                platformPublisher,
                                                                mailVerifier,
                                                                eventPublisher,
                                                                accountAccessor,
                                                                currentTenantAccessor);
        userWorkflowManager = new ProjectUserWorkflowManager(accessUpdateActionFactory);
    }

    private IProjectUserRepository mockUserAccessor() {
        return mock(IProjectUserRepository.class);
    }

    private IEmailVerificationTokenService mockMailVerifier() {
        return mock(IEmailVerificationTokenService.class);
    }

    private IPublisher mockEventPublisher() {
        return mock(IPublisher.class);
    }

    private ApplicationEventPublisher mockPlatformPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    private IAccountsClient mockAccountAccessor() {
        return mock(IAccountsClient.class);
    }

    private IRuntimeTenantResolver mockTenant() {
        IRuntimeTenantResolver tenantResolver = mock(IRuntimeTenantResolver.class);
        when(tenantResolver.getTenant()).thenReturn(TENANT);
        return tenantResolver;
    }

    protected ProjectUser updatedUser() {
        ArgumentCaptor<ProjectUser> userCaptor = ArgumentCaptor.forClass(ProjectUser.class);
        verify(userAccessor).save(userCaptor.capture());
        return userCaptor.getValue();
    }

    protected ApplicationEvent publishedEvent() {
        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(OnInactiveEvent.class);
        verify(platformPublisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }

}
