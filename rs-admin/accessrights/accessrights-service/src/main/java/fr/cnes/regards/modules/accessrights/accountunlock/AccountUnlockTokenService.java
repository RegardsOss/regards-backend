/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.accountunlock;

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

}
