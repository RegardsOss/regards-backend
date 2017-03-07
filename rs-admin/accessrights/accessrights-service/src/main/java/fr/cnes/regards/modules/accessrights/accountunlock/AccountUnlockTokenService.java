/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.accountunlock;

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
    public void create(final Account pAccount) {
        final String uuid = UUID.randomUUID().toString();
        final AccountUnlockToken token = new AccountUnlockToken(uuid, pAccount);
        tokenRepository.save(token);
    }

}
