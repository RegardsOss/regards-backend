/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * This filter allows to inject a public token before JWT authentication filter if no JWT is found and a tenant is
 * specified in request parameters.
 *
 * @author Marc Sordi
 *
 */
public class PublicAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthenticationFilter.class);

    /**
     * Security JWTService
     */
    private final JWTService jwtService;

    public PublicAuthenticationFilter(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain pFilterChain)
            throws ServletException, IOException {

        // Retrieve authentication header
        String authHeader = request.getHeader(HttpConstants.AUTHORIZATION);
        // If no authentication header and no OPTIONS request
        if ((authHeader == null) && !CorsFilter.OPTIONS_REQUEST_TYPE.equals(request.getMethod())) {
            // Try to retrieve target tenant from request
            String tenant = request.getHeader(HttpConstants.SCOPE);
            if (tenant == null) {
                tenant = request.getParameter(HttpConstants.SCOPE);
            }
            // Add authorization header
            CustomHttpServletRequest customRequest = new CustomHttpServletRequest(request);
            addPublicAuthorizationHeader(tenant, customRequest);
            pFilterChain.doFilter(customRequest, response);
        } else {
            // Nothing to do
            pFilterChain.doFilter(request, response);
        }
    }

    /**
     * Add an authorization header for public request
     *
     * @param tenant
     *            tenant
     * @param request
     *            request
     */
    private void addPublicAuthorizationHeader(String tenant, CustomHttpServletRequest request) {
        if (tenant == null) {
            LOGGER.warn("No scope found in request headers or parameters, cannot inject public authorization");
            // Nothing to do if tenant not specified ... authentication filter will fail
            return;
        }

        // Generate a public token
        String jwt = jwtService.generateToken(tenant, "public@regards.com", DefaultRole.PUBLIC.name());
        // Add token into request header
        request.addHeader(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + jwt);
    }
}
