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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Factory that create user access update action for a given user.
 * It hides dependencies for objects using user access update action.
 * It provides an easy way to initialize a UserAccessUpdate
 *
 * @author Thomas Fache
 **/
@Component
public class UserAccessUpdateFactory {

    private final IProjectUserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    private final IPublisher publisher;

    private final IAccountsClient accountsClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public UserAccessUpdateFactory(IProjectUserRepository projectUserRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   IEmailVerificationTokenService emailVerificationTokenService,
                                   IPublisher publisher,
                                   IAccountsClient accountsClient,
                                   IRuntimeTenantResolver runtimeTenantResolver) {
        userRepository = projectUserRepository;
        this.eventPublisher = eventPublisher;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.publisher = publisher;
        this.accountsClient = accountsClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    public AccessWaitingForQualification accessQualification(ProjectUser forUser) {
        return new AccessWaitingForQualification(userRepository, eventPublisher, forUser);
    }

    public MailVerification mailVerification(ProjectUser forUser, LocalDateTime withExpirationDate) {
        return new MailVerification(userRepository, eventPublisher, forUser, withExpirationDate);
    }

    public AccessDeactivation accessDeactivation(ProjectUser forUser) {
        return new AccessDeactivation(userRepository, eventPublisher, forUser);
    }

    public AccessActivation activateAccess(ProjectUser forUser) {
        return new AccessActivation(userRepository, eventPublisher, forUser);
    }

    public AccessDenial denyAccess(ProjectUser forUser) {
        return new AccessDenial(userRepository, eventPublisher, forUser);
    }

    public AccessApproval grantAccess(ProjectUser forUser) {
        return new AccessApproval(userRepository, eventPublisher, forUser);
    }

    public AccessRemoval removeAccess(ProjectUser forUser) {
        return new AccessRemoval(userRepository,
                                 emailVerificationTokenService,
                                 publisher,
                                 accountsClient,
                                 runtimeTenantResolver,
                                 forUser);
    }
}
