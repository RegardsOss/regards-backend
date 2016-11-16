/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class MaintenanceFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    public MaintenanceFilter(JWTService pJwtService) {
        jwtService = pJwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pRequest, HttpServletResponse pResponse,
            FilterChain pFilterChain) throws ServletException, IOException {
        // if it's a GET, request can be done even if the tenant is in maintenance
        if (pRequest.getMethod().equals(HttpMethod.GET.name())) {
            pFilterChain.doFilter(pRequest, pResponse);
        } else {
            String tenant = jwtService.getActualTenant();
            if (MaintenanceManager.getMaintenance(tenant)) {
                pResponse.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Tenant in maintenance");
            } else {
                pFilterChain.doFilter(pRequest, pResponse);
            }
        }

    }

}
