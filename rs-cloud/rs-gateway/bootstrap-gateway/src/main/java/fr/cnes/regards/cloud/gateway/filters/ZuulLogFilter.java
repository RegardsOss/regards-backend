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
package fr.cnes.regards.cloud.gateway.filters;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.net.HttpHeaders;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 *
 * This class is a Zuul proxy filter. It aims to log the HTTP method and the URL.</br>
 * It adds to the request header the X-Forwarded-For field.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Component
public class ZuulLogFilter extends ZuulFilter {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ZuulLogFilter.class);

    public static final String COMMA = ", ";

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String remoteAddr = request.getRemoteAddr();

        LOG.info("Request received : {}@{} from {}", request.getRequestURI(), request.getMethod(), remoteAddr);

        String xForwardedFor = ctx.getZuulRequestHeaders().get(HttpHeaders.X_FORWARDED_FOR);
        if ((xForwardedFor != null) && !xForwardedFor.isEmpty() && !xForwardedFor.contains(remoteAddr)) {
            xForwardedFor = xForwardedFor + COMMA + remoteAddr;
            ctx.getZuulRequestHeaders().put(HttpHeaders.X_FORWARDED_FOR, xForwardedFor);
        } else {
            ctx.getZuulRequestHeaders().put(HttpHeaders.X_FORWARDED_FOR, remoteAddr);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("{} is set to request header : {}", HttpHeaders.X_FORWARDED_FOR,
                      ctx.getZuulRequestHeaders().get(HttpHeaders.X_FORWARDED_FOR));
        }

        return null;
    }

}
