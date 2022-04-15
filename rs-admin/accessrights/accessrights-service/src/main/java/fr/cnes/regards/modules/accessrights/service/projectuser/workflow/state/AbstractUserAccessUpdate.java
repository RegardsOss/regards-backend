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
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

/**
 * A common update action for user access.
 * It is common for all actions except AccessRemoval
 * that is really different because it deletes the ProjectUser.
 *
 * @author Thomas Fache
 **/
public abstract class AbstractUserAccessUpdate implements UserAccessUpdate {

    protected final ProjectUser user;

    private final IProjectUserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;

    protected AbstractUserAccessUpdate(IProjectUserRepository projectUserRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       ProjectUser projectUser) {
        user = projectUser;
        userRepository = projectUserRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public final void updateState() throws EntityOperationForbiddenException {
        validateCurrentAccess();
        doSpecificValidation();
        updateUserAccess(newAccess());
        publishAccessUpdate();
    }

    private void validateCurrentAccess() throws EntityTransitionForbiddenException {
        if (!expectedAccesses().contains(user.getStatus())) {
            throw userIsInAUnexpectedAccess(user);
        }
    }

    protected abstract List<UserStatus> expectedAccesses();

    private EntityTransitionForbiddenException userIsInAUnexpectedAccess(ProjectUser projectUser) {
        return new EntityTransitionForbiddenException(projectUser.getId().toString(),
                                                      ProjectUser.class,
                                                      projectUser.getStatus().toString(),
                                                      Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    protected abstract void doSpecificValidation() throws EntityOperationForbiddenException;

    private void updateUserAccess(UserStatus status) {
        user.setStatus(status);
        userRepository.save(user);
    }

    protected abstract UserStatus newAccess();

    private void publishAccessUpdate() {
        eventToPublish().ifPresent(eventPublisher::publishEvent);
    }

    protected abstract Optional<ApplicationEvent> eventToPublish();
}
