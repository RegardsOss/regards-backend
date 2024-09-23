/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Strings;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Set;

/**
 * This filter allows to inject a public token before JWT authentication filter if no JWT is found and a tenant is
 * specified in request parameters.
 *
 * @author Marc Sordi
 */
public class PublicAuthenticationFilter extends OncePerRequestFilter {

    public static final String PUBLIC_USER_EMAIL = "public@regards.com";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthenticationFilter.class);

    /**
     * Security JWTService
     */
    private final JWTService jwtService;

    private final Set<String> noSecurityRoutes;

    private final AntPathMatcher staticPathMatcher = new AntPathMatcher();

    public PublicAuthenticationFilter(JWTService jwtService, Set<String> noSecurityRoutes) {
        this.jwtService = jwtService;
        this.noSecurityRoutes = noSecurityRoutes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Retrieve authentication header
        String authHeader = request.getHeader(HttpConstants.AUTHORIZATION);
        // If not "Bearer" Authorization and no OPTIONS request => generate PUBLIC token
        if (((authHeader == null) || !authHeader.startsWith(HttpConstants.BEARER))
            && !CorsFilter.OPTIONS_REQUEST_TYPE.equals(request.getMethod())) {
            String tenant = extractScope(request);
            if (tenant != null) {
                // Add authorization header
                CustomHttpServletRequest customRequest = new CustomHttpServletRequest(request);
                addPublicAuthorizationHeader(tenant, customRequest);
                filterChain.doFilter(customRequest, response);
            } else {
                final String message = String.format("[REGARDS PUBLIC FILTER] Missing 'scope' header or param %s@%s",
                                                     request.getServletPath(),
                                                     request.getMethod());
                LOGGER.error(message);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            }
        } else {
            // Nothing to do
            filterChain.doFilter(request, response);
        }
    }

    @Nullable
    private static String extractScope(HttpServletRequest request) {
        // Try to retrieve target tenant from request
        String tenant = request.getHeader(HttpConstants.SCOPE);
        if (Strings.isNullOrEmpty(tenant) && request.getParameter(HttpConstants.SCOPE) != null) {
            // get tenant from request header
            tenant = request.getParameter(HttpConstants.SCOPE);
        }
        return tenant;
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        return noSecurityRoutes.stream()
                               .anyMatch(staticRoute -> staticPathMatcher.match(staticRoute, request.getRequestURI()));
    }

    /**
     * Add an authorization header for public request
     *
     * @param tenant  tenant
     * @param request request
     */
    private void addPublicAuthorizationHeader(String tenant, CustomHttpServletRequest request) {
        if (tenant == null) {
            LOGGER.warn("No scope found in request headers or parameters, cannot inject public authorization");
            // Nothing to do if tenant not specified ... authentication filter will fail
            return;
        }

        // Generate a public token
        String jwt = jwtService.generateToken(tenant, "public", PUBLIC_USER_EMAIL, DefaultRole.PUBLIC.name());
        // Add token into request header
        request.addHeader(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + jwt);
    }
}
