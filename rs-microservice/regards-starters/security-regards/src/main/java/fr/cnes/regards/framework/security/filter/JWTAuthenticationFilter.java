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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import io.jsonwebtoken.ExpiredJwtException;

/**
 * Stateless JWT filter set in the SPRING security chain to authenticate request issuer.<br/>
 * Use {@link JWTAuthenticationProvider} to do it through {@link AuthenticationManager} and its default implementation
 * that delegated authentication to {@link AuthenticationProvider}.
 * @author msordi
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
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        // Clear forced tenant if any
        runtimeTenantResolver.clearTenant();

        // Retrieve authentication header
        String jwt = request.getHeader(HttpConstants.AUTHORIZATION);
        if (jwt == null) {
            // Authorize OPTIONS request
            if (CorsFilter.OPTIONS_REQUEST_TYPE.equals(request.getMethod())) {
                CorsFilter.allowCorsRequest(request, response, filterChain);
            } else {
                final String message = String
                        .format("[REGARDS JWT FILTER] Missing authentication token on %s@%s", request.getServletPath(),
                                request.getMethod());
                LOGGER.error(message);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            }
        } else {

            // Extract JWT from retrieved header
            if (!jwt.startsWith(HttpConstants.BEARER)) {
                final String message = String
                        .format("[REGARDS JWT FILTER] Invalid authentication token on %s@%s", request.getServletPath(),
                                request.getMethod());
                LOGGER.error(message);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            } else {
                jwt = jwt.substring(HttpConstants.BEARER.length()).trim();

                // Init authentication object
                JWTAuthentication jwtAuthentication = new JWTAuthentication(jwt);

                // Try to retrieve target tenant from request
                String tenant = request.getHeader(HttpConstants.SCOPE);
                if (Strings.isNullOrEmpty(tenant) && request.getParameter(HttpConstants.SCOPE) != null) {
                    tenant = request.getParameter(HttpConstants.SCOPE);
                }
                if (!Strings.isNullOrEmpty(tenant)) {
                    jwtAuthentication.setTenant(tenant);
                }

                // Authenticate user with JWT
                try {
                    final Authentication authentication = authenticationManager.authenticate(jwtAuthentication);
                    // Set security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    MDC.put("username", authentication.getName());

                    LOGGER.debug("[REGARDS JWT FILTER] Access granted");

                    // Continue the filtering chain
                    filterChain.doFilter(request, response);
                } catch (AuthenticationException e)  {
                    if (e.getCause() instanceof ExpiredJwtException) {
                        MDC.put("username", ((ExpiredJwtException) e.getCause()).getClaims().getSubject());
                    }
                    throw e;
                }
            }
        }
    }
}
