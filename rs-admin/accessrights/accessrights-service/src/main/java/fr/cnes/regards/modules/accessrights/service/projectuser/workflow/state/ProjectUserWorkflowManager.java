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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Class that enables project user access update.
 * Each access update is validated before its execution.
 * The new state is persisted and a notification is sent to user.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Service
@MultitenantTransactional
@Primary
public class ProjectUserWorkflowManager implements IProjectUserTransitions {

    private final UserAccessUpdateFactory accessUpdateFactory;

    public ProjectUserWorkflowManager(UserAccessUpdateFactory actionFactory) {
        this.accessUpdateFactory = actionFactory;
    }

    @Override
    public void makeWaitForQualification(ProjectUser projectUser) throws EntityOperationForbiddenException {
        accessUpdateFactory.accessQualification(projectUser).updateState();
    }

    @Override
    public void verifyEmail(EmailVerificationToken emailVerificationToken) throws EntityException {
        accessUpdateFactory.mailVerification(emailVerificationToken.getProjectUser(),
                                             emailVerificationToken.getExpiryDate()).updateState();
    }

    @Override
    public void inactiveAccess(final ProjectUser projectUser) throws EntityOperationForbiddenException {
        accessUpdateFactory.accessDeactivation(projectUser).updateState();
    }

    @Override
    public void activeAccess(final ProjectUser projectUser) throws EntityOperationForbiddenException {
        accessUpdateFactory.activateAccess(projectUser).updateState();
    }

    @Override
    public void denyAccess(final ProjectUser projectUser) throws EntityOperationForbiddenException {
        accessUpdateFactory.denyAccess(projectUser).updateState();
    }

    @Override
    public void grantAccess(final ProjectUser projectUser) throws EntityException {
        accessUpdateFactory.grantAccess(projectUser).updateState();
    }

    @Override
    public void removeAccess(final ProjectUser projectUser) {
        accessUpdateFactory.removeAccess(projectUser).updateState();
    }

}
