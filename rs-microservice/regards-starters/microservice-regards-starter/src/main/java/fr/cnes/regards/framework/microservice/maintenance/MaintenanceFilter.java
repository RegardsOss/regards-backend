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
package fr.cnes.regards.framework.microservice.maintenance;

import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.microservice.rest.MaintenanceController;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public class MaintenanceFilter extends OncePerRequestFilter {

    /**
     * REGARDS maintenance HTTP status
     */
    public static final int MAINTENANCE_HTTP_STATUS = 515;

    /**
     * {verb}@{path} request from {x-forwarded-for} was ignored because the service is in Maintenance!
     */
    private static final String REQUEST_IGNORED = "{}@{} request from {} was ignored because the service is in Maintenance!";

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceFilter.class);

    private final IRuntimeTenantResolver resolver;

    private final Set<String> noSecurityRoutes;

    private final AntPathMatcher staticPathMatcher = new AntPathMatcher();

    public MaintenanceFilter(final IRuntimeTenantResolver resolver, Set<String> noSecurityRoutes) {
        this.resolver = resolver;
        this.noSecurityRoutes = noSecurityRoutes;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        // if it's a GET, request can be done even if the tenant is in maintenance
        if (request.getMethod().equals(HttpMethod.GET.name())) {
            filterChain.doFilter(request, response);
        } else {
            // Only authorize to disable maintenance mode
            if (!((request.getRequestURI() != null)
                  && request.getRequestURI()
                             .contains(MaintenanceController.MAINTENANCE_URL)
                  && request.getRequestURI().contains(MaintenanceController.DISABLE))
                && MaintenanceManager.getMaintenance(resolver.getTenant())) {

                String message = String.format("Tenant %s in maintenance!", resolver.getTenant());
                LOGGER.error(message);
                LOGGER.error(REQUEST_IGNORED,
                             request.getMethod(),
                             request.getRequestURI(),
                             request.getHeader(HttpHeaders.X_FORWARDED_FOR));
                response.sendError(MAINTENANCE_HTTP_STATUS, message);
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        return noSecurityRoutes.stream()
                               .anyMatch(staticRoute -> staticPathMatcher.match(staticRoute, request.getRequestURI()));
    }
}
