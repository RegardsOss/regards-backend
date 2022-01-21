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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import org.springframework.stereotype.Component;


/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * WAITING_ACCOUNT_ACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class WaitingAccountActiveState extends AbstractDeletableState {

    public WaitingAccountActiveState(IProjectUserRepository projectUserRepository, IEmailVerificationTokenService emailVerificationTokenService, IPublisher publisher,
            IAccountsClient accountsClient, IRuntimeTenantResolver runtimeTenantResolver
    ) {
        super(projectUserRepository, emailVerificationTokenService, publisher, accountsClient, runtimeTenantResolver);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.workflow.projectuser.AbstractProjectUserState#makeProjectUserWaitForQualification(fr.cnes.regards.modules.accessrights.domain
     * .projects.ProjectUser)
     */
    @Override
    public void makeWaitForQualification(ProjectUser pProjectUser) {
        pProjectUser.setStatus(UserStatus.WAITING_ACCESS);
        getProjectUserRepository().save(pProjectUser);
    }

}
