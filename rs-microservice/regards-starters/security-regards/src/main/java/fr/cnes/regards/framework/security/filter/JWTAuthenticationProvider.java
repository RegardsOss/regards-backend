/*
 * LICENSE_PLACEHOLDER
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
 *
 * @author msordi
 *
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
