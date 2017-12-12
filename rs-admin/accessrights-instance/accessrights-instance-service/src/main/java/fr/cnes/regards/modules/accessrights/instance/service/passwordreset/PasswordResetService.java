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
package fr.cnes.regards.modules.accessrights.instance.service.passwordreset;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.instance.dao.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PasswordResetToken;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountService;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;

/**
 * {@link IPasswordResetService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class PasswordResetService implements IPasswordResetService {

    /**
     * CRUD repository handling {@link PasswordResetToken}s. Autowired by Spring.
     */
    @Autowired
    private final IPasswordResetTokenRepository tokenRepository;

    /**
     * CRUD service handling {@link Account}s. Autowired by Spring.
     */
    @Autowired
    private final IAccountService accountService;

    /**
     * Creates a new instance with passed deps
     *
     * @param pTokenRepository
     *            The verif token repository
     * @param pAccountService
     *            The account service
     */
    public PasswordResetService(final IPasswordResetTokenRepository pTokenRepository,
            final IAccountService pAccountService) {
        super();
        tokenRepository = pTokenRepository;
        accountService = pAccountService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService#getPasswordResetToken(java.lang.String)
     */
    @Override
    public PasswordResetToken getPasswordResetToken(final String pToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pToken)
                .orElseThrow(() -> new EntityNotFoundException(pToken, PasswordResetToken.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.instance.service.IAccountService#createVerificationToken(fr.cnes.regards.
     * modules.accessrights.domain.instance.Account, java.lang.String)
     */
    @Override
    public void createPasswordResetToken(final Account pAccount, final String pToken) {
        final Optional<PasswordResetToken> currentToken = tokenRepository.findByAccount(pAccount);
        if (currentToken.isPresent()) {
            final PasswordResetToken token = currentToken.get();
            token.updateToken(pToken);
            tokenRepository.save(token);
        } else {
            final PasswordResetToken token = new PasswordResetToken(pToken, pAccount);
            tokenRepository.save(token);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.instance.service.IAccountService#changeAccountPassword(java.lang.Long,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void performPasswordReset(final String pAccountEmail, final String pResetCode, final String pNewPassword)
            throws EntityException {
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);
        validatePasswordResetToken(pAccountEmail, pResetCode);
        accountService.changePassword(account.getId(), EncryptionUtils.encryptPassword(pNewPassword));
    }

    /**
     * Validate the password reset token
     *
     * @param pAccountEmail
     *            the account email
     * @param pToken
     *            the token to validate
     * @throws EntityOperationForbiddenException
     *             Thrown if: - token does not exists - is not linked to the passed account - is expired
     */
    private void validatePasswordResetToken(final String pAccountEmail, final String pToken)
            throws EntityOperationForbiddenException {
        // Retrieve the token object
        final PasswordResetToken passToken = tokenRepository.findByToken(pToken)
                .orElseThrow(() -> new EntityOperationForbiddenException(pToken, PasswordResetToken.class,
                        "Token does not exist"));

        // Check same account
        if (!passToken.getAccount().getEmail().equals(pAccountEmail)) {
            throw new EntityOperationForbiddenException(pToken, PasswordResetToken.class, "Invalid token");
        }

        // Check token expiry
        if (passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(pToken, PasswordResetToken.class, "Expired token");
        }
    }

    @Override
    public void deletePasswordResetTokenForAccount(final Account pAccount) {
        final Optional<PasswordResetToken> token = tokenRepository.findByAccount(pAccount);
        if (token.isPresent()) {
            tokenRepository.delete(token.get());
        }
    }

}
