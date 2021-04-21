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
package fr.cnes.regards.modules.accessrights.instance.service.accountunlock;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.AccountUnlockToken;

/**
 * Service managing the account unlock tokens
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccountUnlockTokenService {

    /**
     * Retrieve the {@link AccountUnlockToken} of passed token string
     *
     * @param pToken
     *            the account
     * @return the token
     * @throws EntityNotFoundException
     *             if no {@link AccountUnlockToken} with passed token could be found
     */
    AccountUnlockToken findByToken(final String pToken) throws EntityNotFoundException;

    /**
     * Create a {@link AccountUnlockToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @return generated token
     */
    String create(Account pAccount);

    /**
     * Delete all {@link AccountUnlockToken}s for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     */
    void deleteAllByAccount(Account pAccount);

}
