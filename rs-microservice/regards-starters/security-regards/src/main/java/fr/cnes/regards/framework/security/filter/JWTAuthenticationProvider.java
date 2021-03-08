/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.filter;

import fr.cnes.regards.framework.authentication.IExternalAuthenticationResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * REGARDS JWT provider to authenticate request issuer parsing JWT
 * @author msordi
 */
public class JWTAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationProvider.class);

    private final JWTService jwtService;

    private final IExternalAuthenticationResolver externalAuthenticationResolver;

    public JWTAuthenticationProvider(JWTService pService, IExternalAuthenticationResolver pResolver) {
        jwtService = pService;
        externalAuthenticationResolver = pResolver;
    }

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {
        return Try
            // Fill authentication by parsing JWT token.
            .of(() -> jwtService.parseToken((JWTAuthentication) pAuthentication))
            // If not a REGARDS token, let's try to resolve a Service Provider token.
            .recoverWith(JwtException.class, e -> Try
                // If resolved, a REGARDS token is returned.
                .of(() -> externalAuthenticationResolver.verifyAndAuthenticate(((JWTAuthentication) pAuthentication).getJwt()))
                .peek(token -> LOG.info("Token = {}", token))
                // If not resolved, an (Authentication)Exception is thrown. Drop it, just return that the token is not valid (original exception).
                .recoverWith(Exception.class, ae -> Try.failure(new InsufficientAuthenticationException(e.getMessage(), e)))
                // If resolved, try to parse it again, because it's supposed to be a valid REGARDS token now.
                .mapTry(regardsToken -> jwtService.parseToken(new JWTAuthentication(regardsToken)))
                // If it fails once again, abort (return original exception).
                .recoverWith(JwtException.class, ae -> Try.failure(new InsufficientAuthenticationException(e.getMessage(), e)))
            )
            .get();
    }

    @Override
    public boolean supports(Class<?> pClass) {
        return pClass.isAssignableFrom(JWTAuthentication.class);
    }
}
