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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Add the allow origin in the response headers to allow CORS requests.
 *
 * @author Sébastien Binda
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
     * Options request
     */
    public static final String OPTIONS_REQUEST_TYPE = "OPTIONS";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilter.class);

    /**
     * Method authorization service
     */
    private MethodAuthorizationService methodAuthService = null;

    public CorsFilter() {
        super();
    }

    public CorsFilter(final MethodAuthorizationService pMethodAuthService) {
        super();
        methodAuthService = pMethodAuthService;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            doSecurizedFilter(pRequest, pResponse, pFilterChain);
        } else {
            allowCorsRequest(pRequest, pResponse, pFilterChain);
        }

        if (!OPTIONS_REQUEST_TYPE.equals(pRequest.getMethod())) {
            pFilterChain.doFilter(pRequest, pResponse);
        }

    }

    /**
     *
     * Allow CORS Request only if authenticate user is autorized to.
     *
     * @param pRequest
     *            Http request
     * @param pResponse
     *            Http response
     * @param pFilterChain
     *            Filter chain
     * @throws ServletException
     *             Servlet error
     * @throws IOException
     *             Internal error
     * @since 1.0-SNAPSHOT
     */
    private void doSecurizedFilter(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        // Get authorized ip associated to given role
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();

        @SuppressWarnings("unchecked")
        final Collection<RoleAuthority> roles = (Collection<RoleAuthority>) authentication.getAuthorities();

        allowCorsRequest(pRequest, pResponse, pFilterChain);
        // FIXME Manage CORS
        /**
         * 
         * if (!roles.isEmpty()) { boolean access = false; for (final RoleAuthority role : roles) { final
         * Optional<RoleAuthority> roleAuth = methodAuthService.getRoleAuthority(role.getAuthority(),
         * authentication.getTenant()); if (roleAuth.isPresent()) { access = access || roleAuth.get().getCorsAccess(); }
         * } if (access) { LOG.debug(String.format("[REGARDS CORS FILTER] Access granted for user %s",
         * authentication.getUser().getName())); allowCorsRequest(pRequest, pResponse, pFilterChain); } } else {
         * pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), "[REGARDS CORS FILTER] No Authority Role defined"); }
         */
    }

    /**
     *
     * Allow cors request
     *
     * @param pRequest
     *            Http request
     * @param pResponse
     *            Http response
     * @param pFilterChain
     *            Filter chain
     * @throws ServletException
     *             Servlet error
     * @throws IOException
     *             Internal error
     * @since 1.0-SNAPSHOT
     */
    public static void allowCorsRequest(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws IOException, ServletException {
        pResponse.setHeader(ALLOW_ORIGIN, "*");
        pResponse.setHeader(ALLOW_METHOD, "POST, PUT, GET, OPTIONS, DELETE");
        pResponse.setHeader(ALLOW_HEADER, "authorization, content-type, scope");
        pResponse.setHeader(CONTROL_MAX_AGE, "3600");
    }

}
