package fr.cnes.regards.cloud.gateway.authentication.configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 *
 * Class ResourceServerAuthenticationManager
 *
 * Authentication manager for ResourceServer. Used to decrypt token
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ResourceServerAuthenticationManager implements AuthenticationManager {

    /**
     * JWT service
     */
    private final JWTService jwtService;

    public ResourceServerAuthenticationManager(final JWTService pService) {
        this.jwtService = pService;
    }

    @Override
    public Authentication authenticate(final Authentication pAuthentication) throws AuthenticationException {

        try {
            // Fill authentication parsing JWT token
            final JWTAuthentication auth = jwtService.parseToken((JWTAuthentication) pAuthentication);
            return auth;
        } catch (final JwtException e) {
            throw new InsufficientAuthenticationException(e.getMessage());
        }

    }

}
