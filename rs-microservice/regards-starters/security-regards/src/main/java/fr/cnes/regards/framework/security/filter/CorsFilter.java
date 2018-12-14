/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.base.Strings;

/**
 * Add the allow origin in the response headers to allow CORS requests.
 * @author Sébastien Binda
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

    public CorsFilter(List<String> pAuthorizedAdress) {
        super();
        authorizedAddress = pAuthorizedAdress;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        doSecurisedFilter(request, response, filterChain);

        if (!OPTIONS_REQUEST_TYPE.equals(request.getMethod())) {
            filterChain.doFilter(request, response);
        }

    }

    /**
     * Allow CORS Request only if authenticate user is autorized to.
     * @param request Http request
     * @param response Http response
     * @param pFilterChain Filter chain
     * @throws ServletException Servlet error
     * @throws IOException      Internal error
     */
    private void doSecurisedFilter(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain pFilterChain) throws ServletException, IOException {

        final String originAdress = getClientOrigin(request);

        if ((authorizedAddress == null) || (authorizedAddress.isEmpty())) {
            allowCorsRequest(request, response, pFilterChain);
        } else {
            boolean isAuthorized = false;
            for (final String authorizedAdress : authorizedAddress) {
                isAuthorized = isAuthorized || Pattern.compile(authorizedAdress).matcher(originAdress).matches();
            }
            if (isAuthorized) {
                allowCorsRequest(request, response, pFilterChain);
            } else {
                LOG.error("[CORS REQUEST FILTER] Access denied for client : {}", originAdress);
            }
        }
    }

    /**
     * Return the addresse of the origine request address
     */
    private static String getClientOrigin(HttpServletRequest request) {
        String remoteAddr = null;
        remoteAddr = request.getHeader(REQUEST_HEADER_ORIGIN);
        if (Strings.isNullOrEmpty(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }
        return remoteAddr;
    }

    /**
     * Allow cors request
     * @param request Http request
     * @param response Http response
     * @param filterChain Filter chain
     * @throws ServletException Servlet error
     * @throws IOException      Internal error
     */
    public static void allowCorsRequest(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) {
        response.setHeader(ALLOW_ORIGIN, "*");
        response.setHeader(ALLOW_METHOD, "POST, PUT, GET, OPTIONS, DELETE");
        response.setHeader(ALLOW_HEADER, "authorization, content-type, scope");
        response.setHeader(CONTROL_MAX_AGE, "3600");
    }

}
