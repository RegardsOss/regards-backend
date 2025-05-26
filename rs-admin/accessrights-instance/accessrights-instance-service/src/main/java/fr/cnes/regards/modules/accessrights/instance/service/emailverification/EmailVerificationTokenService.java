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

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.dao.emailverification.IEmailVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationTokenDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * {@link IEmailVerificationTokenService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class EmailVerificationTokenService implements IEmailVerificationTokenService {

    /**
     * CRUD repository handling {@link EmailVerificationToken}s. Autowired by Spring.
     */
    private final IEmailVerificationTokenRepository tokenRepository;

    /**
     * @param tokenRepository the token repository
     */
    public EmailVerificationTokenService(IEmailVerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Create a {@link EmailVerificationToken} for the passed {@link Account}
     *
     * @param account     the account
     * @param originUrl   the origin url
     * @param requestLink the request link
     */
    @Override
    @InstanceTransactional
    public EmailVerificationToken create(Account account, String originUrl, String requestLink) {
        final EmailVerificationToken token = new EmailVerificationToken(account, originUrl, requestLink);
        return tokenRepository.save(token);
    }

    @InstanceTransactional
    @Override
    public EmailVerificationToken importToken(Account account, EmailVerificationTokenDto dto) {
        EmailVerificationToken token = new EmailVerificationToken(account, dto.originUrl(), dto.requestLink());
        token.setToken(dto.token());
        token.setExpiryDate(dto.expiryDate());
        return tokenRepository.save(token);
    }

    @Override
    @InstanceTransactional
    @Transactional(noRollbackFor = EntityNotFoundException.class)
    public EmailVerificationToken findByToken(String emailVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(emailVerificationToken)
                              .orElseThrow(() -> new EntityNotFoundException(emailVerificationToken,
                                                                             EmailVerificationToken.class));
    }

    @Override
    @InstanceTransactional
    @Transactional(noRollbackFor = EntityNotFoundException.class)
    public EmailVerificationToken findByAccount(Account account) throws EntityNotFoundException {
        return tokenRepository.findByAccount(account)
                              .orElseThrow(() -> new EntityNotFoundException(account.getEmail(),
                                                                             EmailVerificationToken.class));
    }

    @Override
    @InstanceTransactional
    public void deleteTokenForAccount(Account account) {
        Optional<EmailVerificationToken> token = tokenRepository.findByAccount(account);
        token.ifPresent(tokenRepository::delete);
    }

}
