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
package fr.cnes.regards.modules.accessrights.instance.service.accountunlock;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.dao.accountunlock.IAccountUnlockTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.AccountUnlockToken;

/**
 * {@link IAccountUnlockTokenService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class AccountUnlockTokenService implements IAccountUnlockTokenService {

    /**
     * CRUD repository handling {@link AccountUnlockToken}s. Autowired by Spring.
     */
    @Autowired
    private final IAccountUnlockTokenRepository tokenRepository;

    /**
     * @param pTokenRepository
     *            the token repository
     */
    public AccountUnlockTokenService(final IAccountUnlockTokenRepository pTokenRepository) {
        super();
        tokenRepository = pTokenRepository;
    }

    @Override
    public AccountUnlockToken findByToken(final String pToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pToken)
                .orElseThrow(() -> new EntityNotFoundException(pToken, AccountUnlockToken.class));
    }

    @Override
    public String create(final Account pAccount) {

        AccountUnlockToken token;

        // Check if a token already exists for the given account. If it already exists, just update validity date.
        final Optional<AccountUnlockToken> alreadyExistsToken = tokenRepository.findByAccount(pAccount);
        if (alreadyExistsToken.isPresent()) {
            token = alreadyExistsToken.get();
            token.updateExipracyDate();
        } else {
            // Else create a new one
            final String uuid = UUID.randomUUID().toString();
            token = new AccountUnlockToken(uuid, pAccount);
        }
        token = tokenRepository.save(token);
        return token.getToken();
    }

    @Override
    public void deleteAllByAccount(Account pAccount) {
        tokenRepository.deleteAllByAccount(pAccount);
    }

}
