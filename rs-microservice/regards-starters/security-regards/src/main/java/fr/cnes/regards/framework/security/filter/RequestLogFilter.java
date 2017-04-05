/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.net.HttpHeaders;

/**
 *
 * This class aims to log the HTTP request received by a microservice.</br>
 * The informations logged are :</br>
 * <li>the request URI,
 * <li>the HTTP method,
 * <li>the IP of the caller or the X-Forwarded-For field extracts from the request header.  
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

        if (LOG.isDebugEnabled()) {
            Enumeration<String> headerNames = pRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = pRequest.getHeader(key);
                LOG.debug("header : {} = {}", key, value);
            }
        }

        String xForwardedFor = pRequest.getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (xForwardedFor != null) {
            LOG.info("Request received : {}@{} from {}", pRequest.getRequestURI(), pRequest.getMethod(), xForwardedFor);
        } else {
            LOG.info("Request received : {}@{} from {}", pRequest.getRequestURI(), pRequest.getMethod(),
                     pRequest.getRemoteAddr());
        }

        pFilterChain.doFilter(pRequest, pResponse);
    }

}
