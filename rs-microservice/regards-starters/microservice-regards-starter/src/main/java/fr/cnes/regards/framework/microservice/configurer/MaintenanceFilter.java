/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.configurer;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.microservice.rest.MaintenanceController;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 *
 */
public class MaintenanceFilter extends OncePerRequestFilter {

    private final IRuntimeTenantResolver resolver;

    public MaintenanceFilter(IRuntimeTenantResolver pResolver) {
        resolver = pResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pRequest, HttpServletResponse pResponse,
            FilterChain pFilterChain) throws ServletException, IOException {
        // if it's a GET, request can be done even if the tenant is in maintenance
        if (pRequest.getMethod().equals(HttpMethod.GET.name())) {
            pFilterChain.doFilter(pRequest, pResponse);
        } else {
            if (!((pRequest.getPathInfo() != null)
                    && pRequest.getPathInfo().contains(MaintenanceController.MAINTENANCES_URL))
                    && MaintenanceManager.getMaintenance(resolver.getTenant())) {
                pResponse.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Tenant in maintenance");
            } else {
                pFilterChain.doFilter(pRequest, pResponse);
            }
        }

    }

}
