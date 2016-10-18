/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.filter;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;

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
        this.jwtService = pService;
    }

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {

        try {
            // Fill authentication parsing JWT token
            final JWTAuthentication auth = jwtService.parseToken((JWTAuthentication) pAuthentication);
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
