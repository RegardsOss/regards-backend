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

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * State pattern implementation defining the actions managing the state of a project user.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public interface IProjectUserTransitions {

    /**
     * Passes the project user from status WAITING_ACCOUNT_ACTIVE to WAITING_ACCESS.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException if the project user is not in status WAITING_ACCOUNT_ACTIVE<br>
     */
    void makeWaitForQualification(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException;

    /**
     * After the user has clicked on the link in the email he received, pass his project user from status WAITING_EMAIL_VERIFICATION to ACCESS_GRANTED.
     *
     * @param pEmailVerificationToken
     * @throws EntityException
     *              <br>
     *             {@link EntityTransitionForbiddenException} when the project user is not in status WAITING_EMAIL_VERIFICATION<br>
     *             {@link EntityNotFoundException} Thrown when no access settings could be found<br>
     */
    void verifyEmail(final EmailVerificationToken pEmailVerificationToken) throws EntityException;

    /**
     * Passes an ACCESS_GRANTED project user to the status ACCESS_INACTIVE.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_GRANTED
     */
    void inactiveAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException;

    /**
     * Passes an ACCESS_INACTIVE project user to the status ACCESS_GRANTED.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_INACTIVE
     */
    void activeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException;

    /**
     * Passes an ACCESS_GRANTED project user to the status ACCESS_DENIED.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_GRANTED
     */
    void denyAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException;

    /**
     * Passes an ACCESS_DENIED project user to the status ACCESS_GRANTED.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityException
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_DENIED
     */
    void grantAccess(final ProjectUser pProjectUser) throws EntityException;

    /**
     * Passes a WAITING_ACCOUNT_ACTIVE/WAITING_ACCESS/ACCESS_GRANTED/ACCESS_INACTIVE/ACCESS_DENIED project user to the status NO_ACCESS and
     * deletes it.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status WAITING_ACCESS/ACCESS_GRANTED/ACCESS_INACTIVE/ACCESS_DENIED
     */
    void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException;

}
