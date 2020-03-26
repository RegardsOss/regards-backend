/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * Abstract state implementation to implement the delete action on a project user.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public abstract class AbstractDeletableState extends AbstractProjectUserState {

    /**
     * Repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service to manage email verification tokens for project users.
     */
    private final IEmailVerificationTokenService emailVerificationTokenService;

    /**
     * AMQP event publisher
     */
    private final IPublisher publisher;

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     * @param publisher
     */
    public AbstractDeletableState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, IPublisher publisher) {
        super();
        projectUserRepository = pProjectUserRepository;
        emailVerificationTokenService = pEmailVerificationTokenService;
        this.publisher = publisher;
    }

    @Override
    public void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        switch (pProjectUser.getStatus()) {
            case WAITING_ACCOUNT_ACTIVE:
            case WAITING_ACCESS:
            case WAITING_EMAIL_VERIFICATION:
            case ACCESS_DENIED:
            case ACCESS_GRANTED:
            case ACCESS_INACTIVE:
                doDelete(pProjectUser);
                break;
            default:
                throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                        pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * Delete a project user
     *
     * @param projectUser
     *            the project user
     */
    protected void doDelete(final ProjectUser projectUser) {
        emailVerificationTokenService.deleteTokenForProjectUser(projectUser);
        projectUserRepository.deleteById(projectUser.getId());
        publisher.publish(new ProjectUserEvent(projectUser.getEmail(), ProjectUserAction.DELETION));
    }

    /**
     * @return the projectUserRepository
     */

    protected IProjectUserRepository getProjectUserRepository() {
        return projectUserRepository;
    }

    /**
     * @return the emailVerificationTokenService
     */
    public IEmailVerificationTokenService getEmailVerificationTokenService() {
        return emailVerificationTokenService;
    }

}
