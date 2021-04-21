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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * WAITING_ACCESS.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class WaitingEmailVerification extends AbstractDeletableState {

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     * @param publisher
     */
    public WaitingEmailVerification(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, IPublisher publisher) {
        super(pProjectUserRepository, pEmailVerificationTokenService, publisher);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AbstractProjectUserState#verifyEmail(fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken)
     */
    @Override
    public void verifyEmail(EmailVerificationToken pEmailVerificationToken) throws EntityException {
        final ProjectUser projectUser = pEmailVerificationToken.getProjectUser();

        if (pEmailVerificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(projectUser.getEmail(), ProjectUser.class,
                    "Verification token has expired");
        }

        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        getProjectUserRepository().save(projectUser);
    }

}
