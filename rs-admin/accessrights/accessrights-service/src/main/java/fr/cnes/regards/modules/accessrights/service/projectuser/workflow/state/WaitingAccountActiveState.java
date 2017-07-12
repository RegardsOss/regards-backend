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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * WAITING_ACCOUNT_ACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class WaitingAccountActiveState extends AbstractDeletableState {

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     */
    public WaitingAccountActiveState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, IPublisher publisher) {
        super(pProjectUserRepository, pEmailVerificationTokenService, publisher);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.workflow.projectuser.AbstractProjectUserState#makeProjectUserWaitForQualification(fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void makeWaitForQualification(ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        pProjectUser.setStatus(UserStatus.WAITING_ACCESS);
        getProjectUserRepository().save(pProjectUser);
    }

}
