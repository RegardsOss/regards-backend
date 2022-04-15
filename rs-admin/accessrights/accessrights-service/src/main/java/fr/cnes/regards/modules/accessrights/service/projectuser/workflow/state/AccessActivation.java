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
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnActiveEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Action to activate the user access to the current project.
 * The user is currently inactive in the current project.
 * The user can now work in the project.
 *
 * @author Thomas Fache
 **/
public class AccessActivation extends AbstractUserAccessUpdate {

    public AccessActivation(IProjectUserRepository projectUserRepository,
                            ApplicationEventPublisher eventPublisher,
                            ProjectUser projectUser) {
        super(projectUserRepository, eventPublisher, projectUser);
    }

    @Override
    protected List<UserStatus> expectedAccesses() {
        return Collections.singletonList(UserStatus.ACCESS_INACTIVE);
    }

    @Override
    protected void doSpecificValidation() throws EntityOperationForbiddenException {
        // Nothing here
    }

    @Override
    protected UserStatus newAccess() {
        return UserStatus.ACCESS_GRANTED;
    }

    @Override
    protected Optional<ApplicationEvent> eventToPublish() {
        return Optional.of(new OnActiveEvent(user));
    }
}
