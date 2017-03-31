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

/**
 *
 * Class RequestLogFilter
 *
 * Filter to log request received by a microservice
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * 
 * @since 1.0-SNAPSHOT
 */
public class RequestLogFilter extends OncePerRequestFilter {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {
        String xForwardedFor = pRequest.getHeader("X-FORWARDED-FOR");

        if (xForwardedFor != null) {
            LOG.info("Request received : {}@{} from {}", pRequest.getRequestURI(), pRequest.getMethod(),
                     xForwardedFor);
        } else {
            LOG.info("Request received : {}@{} from {}", pRequest.getRequestURI(), pRequest.getMethod(), pRequest.getRemoteAddr());
        }

        pFilterChain.doFilter(pRequest, pResponse);
    }

}
