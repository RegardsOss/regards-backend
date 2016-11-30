/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.passwordreset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.instance.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.PasswordResetToken;

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
     * Creates a new instance with passed deps
     *
     * @param pTokenRepository
     *            The verif token repository
     */
    public PasswordResetService(final IPasswordResetTokenRepository pTokenRepository) {
        super();
        tokenRepository = pTokenRepository;
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
     * fr.cnes.regards.modules.accessrights.service.account.IAccountService#createVerificationToken(fr.cnes.regards.
     * modules.accessrights.domain.instance.Account, java.lang.String)
     */
    @Override
    public void createPasswordResetToken(final Account pAccount, final String pToken) {
        final PasswordResetToken token = new PasswordResetToken(pToken, pAccount);
        tokenRepository.save(token);
    }

}
