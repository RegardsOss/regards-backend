/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.microservice.rest.MaintenanceController;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

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

    public MaintenanceFilter(final IRuntimeTenantResolver pResolver) {
        resolver = pResolver;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {
        // if it's a GET, request can be done even if the tenant is in maintenance
        if (pRequest.getMethod().equals(HttpMethod.GET.name())) {
            pFilterChain.doFilter(pRequest, pResponse);
        } else {
            // Only authorize to disable maintenance mode
            if (!((pRequest.getRequestURI() != null) && pRequest.getRequestURI()
                    .contains(MaintenanceController.MAINTENANCE_URL) && pRequest.getRequestURI()
                    .contains(MaintenanceController.DISABLE)) && MaintenanceManager
                    .getMaintenance(resolver.getTenant())) {

                String message = String.format("Tenant %s in maintenance!", resolver.getTenant());
                LOGGER.error(message);
                LOGGER.error(REQUEST_IGNORED, pRequest.getMethod(), pRequest.getRequestURI(),
                             pRequest.getHeader(HttpHeaders.X_FORWARDED_FOR));
                pResponse.sendError(MAINTENANCE_HTTP_STATUS, message);
            } else {
                pFilterChain.doFilter(pRequest, pResponse);
            }
        }
    }

}
