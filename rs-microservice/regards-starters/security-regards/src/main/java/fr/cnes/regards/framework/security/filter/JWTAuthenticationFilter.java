/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.security.domain.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Stateless JWT filter set in the SPRING security chain to authenticate request issuer.<br/>
 * Use {@link JWTAuthenticationProvider} to do it through {@link AuthenticationManager} and its default implementation
 * that delegated authentication to {@link AuthenticationProvider}.
 *
 * @author msordi
 *
 */
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    /**
     * Default security authentication manager
     */
    private final AuthenticationManager authenticationManager;

    /**
     *
     * Constructor
     *
     * @param pAuthenticationManager
     *            Authentication manager
     * @since 1.0-SNAPSHOT
     */
    public JWTAuthenticationFilter(final AuthenticationManager pAuthenticationManager) {
        this.authenticationManager = pAuthenticationManager;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {
        final HttpServletRequest request = pRequest;
        final HttpServletResponse response = pResponse;

        // Retrieve authentication header
        String jwt = request.getHeader(HttpConstants.AUTHORIZATION);
        if (jwt == null) {
            final String message = "[REGARDS JWT FILER] Authentication token missing";
            LOG.error(message);
            response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
        } else {

            // Extract JWT from retrieved header
            if (!jwt.startsWith(HttpConstants.BEARER)) {
                final String message = "[REGARDS JWT FILER] Invalid authentication token";
                LOG.error(message);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            } else {
                jwt = jwt.substring(HttpConstants.BEARER.length()).trim();

                // Init authentication object
                final Authentication jwtAuthentication = new JWTAuthentication(jwt);
                // Authenticate user with JWT
                final Authentication authentication = authenticationManager.authenticate(jwtAuthentication);
                // Set security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                LOG.info("[REGARDS JWT FILER] Access granted");

                // Continue the filtering chain
                pFilterChain.doFilter(request, response);
            }
        }
    }

}
