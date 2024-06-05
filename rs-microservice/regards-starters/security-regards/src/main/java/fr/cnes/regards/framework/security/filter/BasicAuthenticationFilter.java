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

import fr.cnes.regards.framework.security.utils.HttpConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Check that "Basic" Authorization header is valid for authentication endpoint (i.e. ".../oauth/token...")
 * @author Olivier Rousselot
 */
public class BasicAuthenticationFilter  extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthenticationFilter.class);

    private String clientUser;

    private String clientSecret;

    public BasicAuthenticationFilter(String clientUser, String clientSecret) {
        this.clientUser = clientUser;
        this.clientSecret = clientSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Retrieve authentication header
        String authHeader = request.getHeader(HttpConstants.AUTHORIZATION);
        boolean authenticationEndpoint = request.getRequestURI().contains("/oauth/token");
        if (authenticationEndpoint) {
            if (authHeader == null) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
                return;
            }
            authHeader = authHeader.trim();
            // Check that it is a "Basic" authentication...
            if (!StringUtils.startsWithIgnoreCase(authHeader,
                                                 BasicAuthenticationConverter.AUTHENTICATION_SCHEME_BASIC)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Basic Authorization required");
                return;
            }
            // ...and that it contains user/password encoded values
            if (authHeader.equalsIgnoreCase(BasicAuthenticationConverter.AUTHENTICATION_SCHEME_BASIC)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Empty Basic Authorization token");
                return;
            }
            // Decode user/password
            byte[] base64Token = authHeader.substring(6).getBytes(StandardCharsets.UTF_8);
            String token;
            try {
                byte[] decoded = Base64.getDecoder().decode(base64Token);
                token = new String(decoded, StandardCharsets.UTF_8);
                int delim = token.indexOf(":");
                if (delim == -1) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid Basic Authorization token");
                    return;
                }
                String userFromBasic = token.substring(0, delim);
                String secretFromBasic = token.substring(delim + 1);
                if (!userFromBasic.equals(clientUser) || !secretFromBasic.equals(clientSecret)) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid Basic Authorization token");
                    return;
                }
            } catch (IllegalArgumentException ex) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Failed to decode basic authentication token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
