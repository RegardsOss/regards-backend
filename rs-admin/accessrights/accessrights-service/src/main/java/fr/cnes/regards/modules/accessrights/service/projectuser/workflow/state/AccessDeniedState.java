/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status ACCESS_DENIED.
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class AccessDeniedState extends AbstractDeletableState {

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     * @param pEventPublisher
     * @param publisher
     */
    public AccessDeniedState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, ApplicationEventPublisher pEventPublisher,
            IPublisher publisher) {
        super(pProjectUserRepository, pEmailVerificationTokenService, publisher);
        eventPublisher = pEventPublisher;
    }

    @Override
    public void grantAccess(final ProjectUser pProjectUser) {
        pProjectUser.setStatus(UserStatus.WAITING_EMAIL_VERIFICATION);
        getProjectUserRepository().save(pProjectUser);
        eventPublisher.publishEvent(new OnGrantAccessEvent(pProjectUser));
    }

}
