/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.JWTService;
import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;

/**
 * REGARDS JWT provider to authenticate request issuer parsing JWT
 *
 * @author msordi
 *
 */
@Component
public class JWTAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private JWTService jwtService_;

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {

        try {
            // Fill authentication parsing JWT token
            JWTAuthentication auth = jwtService_.parseToken((JWTAuthentication) pAuthentication);
            return auth;
        } catch (InvalidJwtException | MissingClaimException e) {
            throw new InsufficientAuthenticationException(e.getMessage());
        }

    }

    @Override
    public boolean supports(Class<?> pClass) {
        return pClass.isAssignableFrom(JWTAuthentication.class);
    }

}
