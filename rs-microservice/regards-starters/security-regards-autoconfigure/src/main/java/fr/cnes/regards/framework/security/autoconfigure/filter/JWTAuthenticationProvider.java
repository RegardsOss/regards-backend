/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * REGARDS JWT provider to authenticate request issuer parsing JWT
 *
 * @author msordi
 *
 */
@Component
public class JWTAuthenticationProvider implements AuthenticationProvider {

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {

        try {
            // Fill authentication parsing JWT token
            final JWTAuthentication auth = jwtService.parseToken((JWTAuthentication) pAuthentication);
            return auth;
        } catch (JwtException e) {
            throw new InsufficientAuthenticationException(e.getMessage());
        }

    }

    @Override
    public boolean supports(Class<?> pClass) {
        return pClass.isAssignableFrom(JWTAuthentication.class);
    }

}
