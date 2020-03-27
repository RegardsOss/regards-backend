/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * REGARDS JWT provider to authenticate request issuer parsing JWT
 * @author msordi
 */
public class JWTAuthenticationProvider implements AuthenticationProvider {

    /**
     * JWT service
     */
    private final JWTService jwtService;

    public JWTAuthenticationProvider(JWTService pService) {
        jwtService = pService;
    }

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {

        try {
            // Fill authentication parsing JWT token
            return jwtService.parseToken((JWTAuthentication) pAuthentication);
        } catch (JwtException e) {
            throw new InsufficientAuthenticationException(e.getMessage(), e);
        }

    }

    @Override
    public boolean supports(Class<?> pClass) {
        return pClass.isAssignableFrom(JWTAuthentication.class);
    }
}
