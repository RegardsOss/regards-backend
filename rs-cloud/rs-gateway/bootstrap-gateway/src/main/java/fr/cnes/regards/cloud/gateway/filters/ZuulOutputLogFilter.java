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

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import fr.cnes.regards.framwork.logbackappender.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Marc SORDI
 */
@Component
public class ZuulOutputLogFilter extends ZuulFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulOutputLogFilter.class);

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        // Propagate correlation id
        String correlationId = ctx.getRequest().getHeader(ZuulInputLogFilter.CORRELATION_ID);
        ctx.getResponse().addHeader(ZuulInputLogFilter.CORRELATION_ID, correlationId);

        HttpServletRequest request = ctx.getRequest();
        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();
        String remoteAddr = request.getRemoteAddr();

        LOGGER.info(LogConstants.SECURITY_MARKER + "Response ({}) for tracked request {} : {}@{} from {}",
                    ctx.getResponse().getStatus(),
                    correlationId,
                    requestURI,
                    requestMethod,
                    remoteAddr);
        return null;
    }
}
