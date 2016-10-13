/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 *
 * Add the allow origin in the response headers to allow CORS requests.
 *
 * @author CS SI
 * @since 1.0-SNAPSHOT
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest pRequest, HttpServletResponse pResponse,
            FilterChain pFilterChain) throws ServletException, IOException {
        pResponse.setHeader("Access-Control-Allow-Origin", "*");
        pResponse.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
        pResponse.setHeader("Access-Control-Allow-Headers", "authorization, content-type");
        pResponse.setHeader("Access-Control-Max-Age", "3600");

        if (!"OPTIONS".equals(pRequest.getMethod())) {
            pFilterChain.doFilter(pRequest, pResponse);
        }
    }
}
