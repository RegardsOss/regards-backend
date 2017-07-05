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
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    /**
     * Default security authentication manager
     */
    private final AuthenticationManager authenticationManager;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public JWTAuthenticationFilter(final AuthenticationManager authenticationManager,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.authenticationManager = authenticationManager;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        // Retrieve authentication header
        String jwt = pRequest.getHeader(HttpConstants.AUTHORIZATION);
        if (jwt == null) {
            // Authorize OPTIONS request
            if (CorsFilter.OPTIONS_REQUEST_TYPE.equals(pRequest.getMethod())) {
                CorsFilter.allowCorsRequest(pRequest, pResponse, pFilterChain);
            } else {
                final String message = "[REGARDS JWT FILTER] Missing authentication token";
                LOGGER.error(message);
                pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            }
        } else {

            // Extract JWT from retrieved header
            if (!jwt.startsWith(HttpConstants.BEARER)) {
                final String message = "[REGARDS JWT FILTER] Invalid authentication token";
                LOGGER.error(message);
                pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            } else {
                jwt = jwt.substring(HttpConstants.BEARER.length()).trim();

                // Init authentication object
                final Authentication jwtAuthentication = new JWTAuthentication(jwt);
                // Authenticate user with JWT
                final Authentication authentication = authenticationManager.authenticate(jwtAuthentication);
                // Set security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Clear forced tenant if any
                runtimeTenantResolver.clearTenant();

                MDC.put("tenant", runtimeTenantResolver.getTenant());

                LOGGER.debug("[REGARDS JWT FILTER] Access granted");

                // Continue the filtering chain
                pFilterChain.doFilter(pRequest, pResponse);
            }
        }
    }
}
