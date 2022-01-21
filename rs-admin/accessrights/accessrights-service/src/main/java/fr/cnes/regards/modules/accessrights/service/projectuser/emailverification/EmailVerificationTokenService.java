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
package fr.cnes.regards.modules.accessrights.service.projectuser.emailverification;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * {@link IEmailVerificationTokenService} implementation.
 * @author Xavier-Alexandre Brochard
 */
@Service
@MultitenantTransactional
public class EmailVerificationTokenService implements IEmailVerificationTokenService {

    /**
     * CRUD repository handling {@link EmailVerificationToken}s. Autowired by Spring.
     */
    @Autowired
    private final IVerificationTokenRepository tokenRepository;

    /**
     * @param pTokenRepository the token repository
     */
    public EmailVerificationTokenService(final IVerificationTokenRepository pTokenRepository) {
        super();
        tokenRepository = pTokenRepository;
    }

    @Override
    public ProjectUser getProjectUserByToken(final String pVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pVerificationToken)
                .orElseThrow(() -> new EntityNotFoundException(pVerificationToken, EmailVerificationToken.class))
                .getProjectUser();
    }

    /**
     * Create a {@link EmailVerificationToken} for the passed {@link ProjectUser}
     * @param pProjectUser the project user
     * @param pOriginUrl the origin url
     * @param pRequestLink the request link
     */
    @Override
    public void create(final ProjectUser pProjectUser, final String pOriginUrl, final String pRequestLink) {
        final EmailVerificationToken token = new EmailVerificationToken(pProjectUser, pOriginUrl, pRequestLink);
        tokenRepository.save(token);
    }

    @Override
    public EmailVerificationToken findByToken(final String pEmailVerificationToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pEmailVerificationToken)
                .orElseThrow(() -> new EntityNotFoundException(pEmailVerificationToken, EmailVerificationToken.class));
    }

    @Override
    public EmailVerificationToken findByProjectUser(final ProjectUser pProjectUser) throws EntityNotFoundException {
        return tokenRepository.findByProjectUser(pProjectUser)
                .orElseThrow(() -> new EntityNotFoundException(pProjectUser.getEmail(), EmailVerificationToken.class));
    }

    @Override
    public void deleteTokenForProjectUser(final ProjectUser pProjectUser) {
        Optional<EmailVerificationToken> token = tokenRepository.findByProjectUser(pProjectUser);
        token.ifPresent(tokenRepository::delete);

    }
}
