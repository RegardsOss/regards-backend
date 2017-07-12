/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.account.accountunlock;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.accountunlock.IAccountUnlockTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

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

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.accountunlock.IAccountUnlockTokenService#getAccountUnlockToken(java.lang.
     * String)
     */
    @Override
    public AccountUnlockToken findByToken(final String pToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pToken)
                .orElseThrow(() -> new EntityNotFoundException(pToken, AccountUnlockToken.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.accountunlock.IAccountUnlockTokenService#createAccountUnlockToken(fr.cnes.
     * regards .modules.accessrights.domain.instance.Account, java.lang.String)
     */
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

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.service.account.accountunlock.IAccountUnlockTokenService#deleteAllByAccount(fr.cnes.regards.modules.accessrights.domain.instance.Account)
     */
    @Override
    public void deleteAllByAccount(Account pAccount) {
        tokenRepository.deleteAllByAccount(pAccount);
    }

}
