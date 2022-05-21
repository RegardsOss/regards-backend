/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.authentication.internal.filter;

import fr.cnes.regards.framework.security.filter.IpFilter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * Class RoleSysFilter
 * <p>
 * Specific gateway Filter to deny access to all Systems Roles. Systems roles must be used between microservices only.
 *
 * @author Sébastien Binda
 */
public class RoleSysFilter extends OncePerRequestFilter {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IpFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Get authorized ip associated to given role
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();

        @SuppressWarnings("unchecked")
        Collection<RoleAuthority> roles = (Collection<RoleAuthority>) authentication.getAuthorities();

        boolean isSysRole = false;
        for (RoleAuthority role : roles) {
            if (RoleAuthority.isSysRole(role.getAuthority())) {
                isSysRole = true;
                String message = "[REGARDS FILTER] - Authorization denied for SYS Roles";
                LOGGER.error(message);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
                break;
            }
        }

        if (!isSysRole) {
            filterChain.doFilter(request, response);
        }

    }

}
