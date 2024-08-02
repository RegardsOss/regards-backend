/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.projectuser.emailverification;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Interface defining the service managing the email verification tokens
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IEmailVerificationTokenService {

    /**
     * Create an email verification token with passed attributes
     *
     * @param pProjectUser the project user
     * @param pOriginUrl   Necessary to the frontend for redirecting the user after he clicked on the email validation link.
     * @param pRequestLink Also necessary to the frontend for redirecting the user after he clicked on the email validation link.
     */
    void create(final ProjectUser pProjectUser, final String pOriginUrl, final String pRequestLink);

    /**
     * Retrieve the email verification token by token
     *
     * @param pEmailVerificationToken the token
     * @return the token
     * @throws EntityNotFoundException if the token could not be foud
     */
    EmailVerificationToken findByToken(final String pEmailVerificationToken) throws EntityNotFoundException;

    /**
     * Retrieve the email verification token by account
     *
     * @param pProjectUser the account
     * @return the token
     * @throws EntityNotFoundException if the token could not be foud
     */
    EmailVerificationToken findByProjectUser(final ProjectUser pProjectUser) throws EntityNotFoundException;

    /**
     * Return the account linked to the passed email verification token
     *
     * @param pEmailVerificationToken the token
     * @return the account
     * @throws EntityNotFoundException if the token could not be found
     */
    ProjectUser getProjectUserByToken(String pEmailVerificationToken) throws EntityNotFoundException;

    /**
     * Delete a {@link EmailVerificationToken} for the passed {@link ProjectUser}
     *
     * @param pProjectUser the project user
     */
    void deleteTokenForProjectUser(final ProjectUser pProjectUser);

    /**
     * Generate a new token for the given project user.
     *
     * @param pProjectUser the project user.
     * @throws EntityNotFoundException if the token could not be found
     */
    void renewToken(final ProjectUser pProjectUser) throws EntityNotFoundException;

}
