/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.emailverification;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

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
    @Autowired
    private final IVerificationTokenRepository tokenRepository;

    /**
     * @param pTokenRepository
     *            the token repository
     */
    public EmailVerificationTokenService(final IVerificationTokenRepository pTokenRepository) {
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
    public ProjectUser getProjectUserByToken(final String pVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pVerificationToken)
                .orElseThrow(() -> new EntityNotFoundException(pVerificationToken, EmailVerificationToken.class))
                .getProjectUser();
    }

    /**
     * Create a {@link EmailVerificationToken} for the passed {@link ProjectUser}
     *
     * @param pProjectUser
     *            the project user
     * @param pOriginUrl
     *            the origin url
     * @param pRequestLink
     *            the request link
     */
    @Override
    public void create(final ProjectUser pProjectUser, final String pOriginUrl, final String pRequestLink) {
        final EmailVerificationToken token = new EmailVerificationToken(pProjectUser, pOriginUrl, pRequestLink);
        tokenRepository.save(token);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#getVerificationToken(java.lang.String)
     */
    @Override
    public EmailVerificationToken findByToken(final String pEmailVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pEmailVerificationToken)
                .orElseThrow(() -> new EntityNotFoundException(pEmailVerificationToken, EmailVerificationToken.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.registration.IVerificationTokenService#findByAccount(fr.cnes.regards.modules
     * .accessrights.domain.instance.Account)
     */
    @Override
    public EmailVerificationToken findByProjectUser(final ProjectUser pProjectUser) throws EntityNotFoundException {
        return tokenRepository.findByProjectUser(pProjectUser)
                .orElseThrow(() -> new EntityNotFoundException(pProjectUser.getEmail(), EmailVerificationToken.class));
    }

    @Override
    public void deleteTokenForProjectUser(final ProjectUser pProjectUser) {
        final Optional<EmailVerificationToken> token = tokenRepository.findByProjectUser(pProjectUser);
        if (token.isPresent()) {
            tokenRepository.delete(token.get());
        }

    }
}
