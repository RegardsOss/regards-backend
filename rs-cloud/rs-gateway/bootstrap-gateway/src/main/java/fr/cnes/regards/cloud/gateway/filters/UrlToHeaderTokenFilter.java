/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * This filter detects JWT in the URL query params and if found set it into the header.<br/>
 * Source URL query param : token<br/>
 * Target header : Bearer : Authorization
 * @author Marc Sordi
 */
@Component
public class UrlToHeaderTokenFilter extends ZuulFilter {

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Authorization header scheme
     */
    public static final String BEARER = "Bearer";

    /**
     * Token that may be passed through request query parameter for download purpose
     */
    public static final String TOKEN = "token";

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

        // Try to retrieve JWT
        String jwt = request.getParameter(TOKEN);

        // Inject into header if not null and bearer not already set
        if ((jwt != null) && (request.getParameter(BEARER) == null)) {
            ctx.getZuulRequestHeaders().put(AUTHORIZATION, BEARER + " " + jwt);
        }

        return null;
    }

}
