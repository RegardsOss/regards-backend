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

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Action to approve the user access to the current project.
 * The user is currently denied or waiting for access
 * The user is now waiting for an email verification.
 *
 * @author Thomas Fache
 **/
public class AccessApproval extends AbstractUserAccessUpdate {

    protected AccessApproval(IProjectUserRepository projectUserRepository,
                             ApplicationEventPublisher eventPublisher,
                             ProjectUser projectUser) {
        super(projectUserRepository, eventPublisher, projectUser);
    }

    @Override
    protected List<UserStatus> expectedAccesses() {
        return Arrays.asList(UserStatus.WAITING_ACCESS, UserStatus.ACCESS_DENIED);
    }

    @Override
    protected void doSpecificValidation() throws EntityOperationForbiddenException {
        // Nothing here
    }

    @Override
    protected UserStatus newAccess() {
        return UserStatus.WAITING_EMAIL_VERIFICATION;
    }

    @Override
    protected Optional<ApplicationEvent> eventToPublish() {
        return Optional.of(new OnGrantAccessEvent(user));
    }
}
