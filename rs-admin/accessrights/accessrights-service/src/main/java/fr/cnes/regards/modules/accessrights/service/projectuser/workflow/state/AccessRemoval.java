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
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserEvent;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * Action to remove a user from the current project.
 * Delete the user and remove its access from the project.
 *
 * @author Thomas Fache
 **/
public class AccessRemoval implements UserAccessUpdate {

    private final IProjectUserRepository userRepository;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    private final IPublisher publisher;

    private final IAccountsClient accountsClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ProjectUser user;

    public AccessRemoval(IProjectUserRepository projectUserRepository,
                         IEmailVerificationTokenService emailVerificationTokenService,
                         IPublisher publisher,
                         IAccountsClient accountsClient,
                         IRuntimeTenantResolver runtimeTenantResolver,
                         ProjectUser forUser) {
        userRepository = projectUserRepository;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.publisher = publisher;
        this.accountsClient = accountsClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
        user = forUser;
    }

    @Override
    public void updateState() {
        try {
            FeignSecurityManager.asSystem();
            accountsClient.unlink(user.getEmail(), runtimeTenantResolver.getTenant());
        } finally {
            FeignSecurityManager.reset();
        }
        emailVerificationTokenService.deleteTokenForProjectUser(user);
        userRepository.deleteById(user.getId());
        publisher.publish(new ProjectUserEvent(user.getEmail(), ProjectUserAction.DELETE));
    }
}
