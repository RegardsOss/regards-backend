/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

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
     * Request Header forward for parameter
     */
    public static final String REQUEST_HEADER_ORIGIN = "Origin";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilter.class);

    /**
     * List of authorized adresses for CORS requests.
     */
    private List<String> authorizedAddress;

    public CorsFilter() {
        super();
    }

    public CorsFilter(final List<String> pAuthorizedAdress) {
        super();
        authorizedAddress = pAuthorizedAdress;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        doSecurisedFilter(pRequest, pResponse, pFilterChain);

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
    private void doSecurisedFilter(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        final String originAdress = getClientOrigin(pRequest);

        if ((authorizedAddress == null) || (authorizedAddress.size() == 0)) {
            allowCorsRequest(pRequest, pResponse, pFilterChain);
        } else {
            boolean isAuthorized = false;
            for (final String authorizedAdress : authorizedAddress) {
                isAuthorized = isAuthorized || Pattern.compile(authorizedAdress).matcher(originAdress).matches();
            }
            if (isAuthorized) {
                allowCorsRequest(pRequest, pResponse, pFilterChain);
            } else {
                LOG.error("[CORS REQUEST FILTER] Access denied for client : {}", originAdress);
            }
        }
    }

    /**
     *
     * Return the addresse of the origine request address
     *
     * @param request
     * @return
     * @since 1.0-SNAPSHOT
     */
    private static String getClientOrigin(final HttpServletRequest request) {
        String remoteAddr = null;
        if (request != null) {
            remoteAddr = request.getHeader(REQUEST_HEADER_ORIGIN);
            if ((remoteAddr == null) || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
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
