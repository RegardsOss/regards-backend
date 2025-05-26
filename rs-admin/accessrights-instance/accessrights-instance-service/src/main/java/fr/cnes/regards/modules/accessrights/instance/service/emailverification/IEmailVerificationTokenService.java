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
package fr.cnes.regards.modules.accessrights.instance.service.emailverification;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationTokenDto;

/**
 * Interface defining the service managing the email verification tokens
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IEmailVerificationTokenService {

    /**
     * Create an email verification token with passed attributes
     *
     * @param account     the account
     * @param originUrl   Necessary to the frontend for redirecting the user after he clicked on the email validation link.
     * @param requestLink Also necessary to the frontend for redirecting the user after he clicked on the email validation link.
     */
    EmailVerificationToken create(final Account account, final String originUrl, final String requestLink);

    /**
     * Import an email verification token with passed attributes. Used to migrate email verification tokens
     * from rs-admin to rs-admin-instance.
     *
     * @param account the account
     * @param dto     the token attributes
     */
    EmailVerificationToken importToken(Account account, EmailVerificationTokenDto dto);

    /**
     * Retrieve the email verification token by token
     *
     * @param emailVerificationToken the token
     * @return the token
     * @throws EntityNotFoundException if the token could not be found
     */
    EmailVerificationToken findByToken(final String emailVerificationToken) throws EntityNotFoundException;

    /**
     * Retrieve the email verification token by account
     *
     * @param account the account
     * @return the token
     * @throws EntityNotFoundException if the token could not be foud
     */
    EmailVerificationToken findByAccount(final Account account) throws EntityNotFoundException;

    /**
     * Delete a {@link EmailVerificationToken} for the passed {@link Account}
     *
     * @param account the account
     */
    void deleteTokenForAccount(final Account account);

}
