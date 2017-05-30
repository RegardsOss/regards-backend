/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.configurer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.microservice.rest.MaintenanceController;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 *
 */
public class MaintenanceFilter extends OncePerRequestFilter {

    /**
     * {verb}@{path} request from {x-forwarded-for} was ignored because the service is in Maintenance!
     */
    private static String REQUEST_IGNORED="{}@{} request from {} was ignored because the service is in Maintenance!";

    private static Logger LOG= LoggerFactory.getLogger(MaintenanceFilter.class);

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
            if (!((pRequest.getRequestURI() != null)
                    && pRequest.getRequestURI().contains(MaintenanceController.MAINTENANCE_URL)
                    && pRequest.getRequestURI().contains(MaintenanceController.DISABLE))
                    && MaintenanceManager.getMaintenance(resolver.getTenant())) {
                LOG.error(REQUEST_IGNORED, pRequest.getMethod(), pRequest.getRequestURI(), pRequest.getHeader(
                        HttpHeaders.X_FORWARDED_FOR));
                pResponse.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Tenant in maintenance");
            } else {
                pFilterChain.doFilter(pRequest, pResponse);
            }
        }

    }

}
