/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 *
 * Add the allow origin in the response headers to allow CORS requests.
 *
 * @author CS SI
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

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {
        pResponse.setHeader(ALLOW_ORIGIN, "*");
        pResponse.setHeader(ALLOW_METHOD, "POST, PUT, GET, OPTIONS, DELETE");
        pResponse.setHeader(ALLOW_HEADER, "authorization, content-type");
        pResponse.setHeader(CONTROL_MAX_AGE, "3600");

        if (!"OPTIONS".equals(pRequest.getMethod())) {
            pFilterChain.doFilter(pRequest, pResponse);
        }
    }
}
