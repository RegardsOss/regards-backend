/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService;

/**
 * {@link IPasswordResetService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class VerificationTokenService implements IVerificationTokenService {

    /**
     * CRUD repository handling {@link VerificationToken}s. Autowired by Spring.
     */
    @Autowired
    private final IVerificationTokenRepository tokenRepository;

    /**
     * @param pTokenRepository
     *            the token repository
     */
    public VerificationTokenService(final IVerificationTokenRepository pTokenRepository) {
        super();
        tokenRepository = pTokenRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountService#getAccountByVerificationToken(java.lang.
     * String)
     */
    @Override
    public Account getAccountByVerificationToken(final String pVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pVerificationToken)
                .orElseThrow(() -> new EntityNotFoundException(pVerificationToken, VerificationToken.class))
                .getAccount();
    }

    /**
     * Create a {@link VerificationToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pOriginUrl
     *            the origin url
     * @param pRequestLink
     *            the request link
     */
    @Override
    public void create(final Account pAccount, final String pOriginUrl, final String pRequestLink) {
        final VerificationToken token = new VerificationToken(pAccount, pOriginUrl, pRequestLink);
        tokenRepository.save(token);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#getVerificationToken(java.lang.String)
     */
    @Override
    public VerificationToken findByToken(final String pVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pVerificationToken)
                .orElseThrow(() -> new EntityNotFoundException(pVerificationToken, VerificationToken.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.registration.IVerificationTokenService#findByAccount(fr.cnes.regards.modules
     * .accessrights.domain.instance.Account)
     */
    @Override
    public VerificationToken findByAccount(final Account pAccount) throws EntityNotFoundException {
        return tokenRepository.findByAccount(pAccount)
                .orElseThrow(() -> new EntityNotFoundException(pAccount.getEmail(), VerificationToken.class));
    }

    @Override
    public void deletePasswordResetTokenForAccount(final Account pAccount) {
        final Optional<VerificationToken> token = tokenRepository.findByAccount(pAccount);
        if (token.isPresent()) {
            tokenRepository.delete(token.get());
        }

    }
}
