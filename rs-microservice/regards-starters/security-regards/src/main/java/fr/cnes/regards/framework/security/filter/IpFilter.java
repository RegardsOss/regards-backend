/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Class IPFilter
 *
 * Spring MVC request filter by IP
 * @author sbinda
 */
public class IpFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(IpFilter.class);

    /**
     * Provider of authorities entities
     */
    private final MethodAuthorizationService methodAuthService;

    /**
     * Constructor
     */
    public IpFilter(MethodAuthorizationService methodAuthService) {
        super();
        this.methodAuthService = methodAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Get authorized ip associated to given role
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();

        @SuppressWarnings("unchecked") Collection<RoleAuthority> roles = (Collection<RoleAuthority>) authentication
                .getAuthorities();

        if (!roles.isEmpty()) {
            List<String> authorizedAddresses = retrieveRoleAuthorizedAddresses(roles, authentication.getTenant());
            if (!checkAccessByAddress(authorizedAddresses, request.getRemoteAddr())) {
                String message = String
                        .format("[REGARDS IP FILTER] - %s - Authorization denied", request.getRemoteAddr());
                LOG.error(message);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            } else {
                LOG.debug(String.format("[REGARDS IP FILTER] - %s - Authorization granted", request.getRemoteAddr()));

                // Continue the filtering chain
                filterChain.doFilter(request, response);
            }
        } else {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "No Authority Role defined");
        }
    }

    /**
     * Retrieve authorized addresses for the given roles.
     * @param roles roles
     * @return authorized addresses
     * @throws SecurityException Error retrieving role informations
     */
    private List<String> retrieveRoleAuthorizedAddresses(Collection<RoleAuthority> roles, String tenant) {
        List<String> authorizedAddresses = new ArrayList<>();
        for (RoleAuthority role : roles) {
            // Role is a sys role then there is no ip limitation
            if (!RoleAuthority.isSysRole(role.getAuthority()) && !RoleAuthority
                    .isInstanceAdminRole(role.getAuthority())) {
                Optional<RoleAuthority> roleAuth = methodAuthService
                        .getRoleAuthority(RoleAuthority.getRoleName(role.getAuthority()), tenant);
                roleAuth.ifPresent(r -> authorizedAddresses.addAll(r.getAuthorizedIpAdresses()));
            }
        }
        return authorizedAddresses;
    }

    /**
     * Check if the user adress match ones of the role authorized addresses.
     * @param inAuthorizedAddress Role authorized addresses
     * @param userAdress user address
     * @return [true|false]
     */
    private boolean checkAccessByAddress(List<String> inAuthorizedAddress, String userAdress) {
        boolean accessAuthorized = false;
        if ((inAuthorizedAddress != null) && !inAuthorizedAddress.isEmpty()) {
            if ((userAdress != null) && !userAdress.isEmpty()) {
                for (String authorizedAddress : inAuthorizedAddress) {
                    Pattern pattern = Pattern.compile(authorizedAddress);
                    accessAuthorized |= pattern.matcher(userAdress).matches();
                }
            }
        } else {
            accessAuthorized = true;
        }
        return accessAuthorized;
    }

}
