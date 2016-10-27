/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Add the allow origin in the response headers to allow CORS requests.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class CorsFilter extends OncePerRequestFilter {

    /**
     * Http Request Header
     */
    public static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * Http Request Header
     */
    public static final String ALLOW_METHOD = "Access-Control-Allow-Methods";

    /**
     * Http Request Header
     */
    public static final String ALLOW_HEADER = "Access-Control-Allow-Headers";

    /**
     * Http Request Header
     */
    public static final String CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilter.class);

    /**
     * Provider of authorities entities
     */
    private final IAuthoritiesProvider authoritiesProvider;

    public CorsFilter(final IAuthoritiesProvider pAuthoritiesProvider) {
        super();
        authoritiesProvider = pAuthoritiesProvider;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        // Get authorized ip associated to given role
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();

        @SuppressWarnings("unchecked")
        final Collection<RoleAuthority> roles = (Collection<RoleAuthority>) authentication.getAuthorities();

        if (!roles.isEmpty()) {
            boolean access = false;
            for (final RoleAuthority role : roles) {
                access = access || authoritiesProvider.hasCorsRequestsAccess(role.getAuthority());
            }
            if (!access) {
                pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), String
                        .format("[REGARDS CORS FILTER] Access denied for user %s", authentication.getUser().getName()));
            } else {
                LOG.info(String.format("[REGARDS CORS FILTER] Access granted for user %s",
                                       authentication.getUser().getName()));
                pResponse.setHeader(ALLOW_ORIGIN, "*");
                pResponse.setHeader(ALLOW_METHOD, "POST, PUT, GET, OPTIONS, DELETE");
                pResponse.setHeader(ALLOW_HEADER, "authorization, content-type");
                pResponse.setHeader(CONTROL_MAX_AGE, "3600");

                if (!"OPTIONS".equals(pRequest.getMethod())) {
                    pFilterChain.doFilter(pRequest, pResponse);
                }
            }
        } else {
            pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), "[REGARDS CORS FILTER] No Authority Role defined");
        }
    }
}
