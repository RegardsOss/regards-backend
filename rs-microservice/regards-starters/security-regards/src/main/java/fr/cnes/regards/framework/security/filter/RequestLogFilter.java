/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This class aims to log the HTTP request received by a microservice.</br>
 * The information logged is :</br>
 * <li>the request URI,
 * <li>the HTTP method,
 * <li>the IP of the caller or the X-Forwarded-For field extracts from the request header.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public class RequestLogFilter extends OncePerRequestFilter {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {

        if (LOG.isDebugEnabled()) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                LOG.debug("header : {} = {}", key, value);
            }
        }

        String xForwardedFor = request.getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (xForwardedFor != null) {
            LOG.info("Request received : {}@{} from {}", request.getRequestURI(), request.getMethod(), xForwardedFor);
        } else {
            LOG.info("Request received : {}@{} from {}",
                     request.getRequestURI(),
                     request.getMethod(),
                     request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }

}
