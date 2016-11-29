/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;

/**
 * {@link IRegistrationService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class RegistrationService implements IRegistrationService {

    /**
     * CRUD repository handling {@link VerificationToken}s. Autowired by Spring.
     */
    @Autowired
    private final IVerificationTokenRepository tokenRepository;

    /**
     * Creates a new instance with passed deps
     *
     * @param pTokenRepository
     *            The verif token repository
     */
    public RegistrationService(final IVerificationTokenRepository pTokenRepository) {
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
    public Account getAccountByVerificationToken(final String pVerificationToken) {
        return tokenRepository.findByToken(pVerificationToken).getAccount();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountService#createVerificationToken(fr.cnes.regards.
     * modules.accessrights.domain.instance.Account, java.lang.String)
     */
    @Override
    public void createVerificationToken(final Account pAccount, final String pToken) {
        final VerificationToken token = new VerificationToken(pToken, pAccount);
        tokenRepository.save(token);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#getVerificationToken(java.lang.String)
     */
    @Override
    public VerificationToken getVerificationToken(final String pVerificationToken) {
        return tokenRepository.findByToken(pVerificationToken);
    }
}
