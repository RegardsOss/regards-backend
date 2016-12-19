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
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

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
     * Security JWTService
     */
    private final JWTService jwtService;

    /**
     *
     * Constructor
     *
     * @param pAuthenticationManager
     *            Authentication manager
     * @param pJwtService
     *            security JWT Service
     * @since 1.0-SNAPSHOT
     */
    public JWTAuthenticationFilter(final AuthenticationManager pAuthenticationManager, final JWTService pJwtService) {
        jwtService = pJwtService;
        authenticationManager = pAuthenticationManager;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {
        final HttpServletRequest request = pRequest;
        final HttpServletResponse response = pResponse;

        // Retrieve authentication header
        String jwt = request.getHeader(HttpConstants.AUTHORIZATION);
        if (jwt == null) {
            // Authorize OPTIONS request
            if (CorsFilter.OPTIONS_REQUEST_TYPE.equals(request.getMethod())) {
                CorsFilter.allowCorsRequest(pRequest, pResponse, pFilterChain);
            } else {
                generatePublicToken(pRequest, pResponse, pFilterChain);
            }
        } else {

            // Extract JWT from retrieved header
            if (!jwt.startsWith(HttpConstants.BEARER)) {
                final String message = "[REGARDS JWT FILTER] Invalid authentication token";
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

                LOG.debug("[REGARDS JWT FILTER] Access granted");

                // Continue the filtering chain
                pFilterChain.doFilter(request, response);
            }
        }
    }

    /**
     *
     * Generate a public token
     *
     * @param pRequest
     *            HTTP request
     * @param pResponse
     *            HTTP response
     * @param pFilterChain
     *            all filters to apply next
     * @throws IOException
     *             Error in HTTP response generation
     * @throws ServletException
     *             Error token generation
     * @throws JwtException
     * @since 1.0-SNAPSHOT
     */
    private void generatePublicToken(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws IOException {

        String scope = pRequest.getHeader(HttpConstants.SCOPE);
        if (scope == null) {
            scope = pRequest.getParameter(HttpConstants.SCOPE);
        }

        try {
            if (scope == null) {
                final String message = "[REGARDS JWT FILTER] Authentication token missing and no scope defined for public access.";
                LOG.error(message);
                pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            } else {
                // Generate a token with PUBLIC Role
                jwtService.injectToken(scope, DefaultRole.PUBLIC.name(), "public@regards.com");
                pFilterChain.doFilter(pRequest, pResponse);
            }
        } catch (final JwtException | ServletException e) {
            final String message = String.format("[REGARDS JWT FILTER] Public token generation failed. %s",
                                                 e.getMessage());
            LOG.error(e.getMessage(), e);
            LOG.error(message);
            pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), message);
        }

    }

}
