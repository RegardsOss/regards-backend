/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.netflix.zuul.context.RequestContext;

import fr.cnes.regards.framework.proxy.ProxyConfiguration;

/**
 * @author sbinda
 *
 */
@Component
@AutoConfigureAfter(value = ProxyConfiguration.class)
public class SimpleHostRoutingWithProxyFilter extends SimpleHostRoutingFilter {

    @Autowired(required = false)
    private HttpClient httpClient;

    public static final String HEADER_HOST = "Host";

    /**
     * @param helper
     * @param properties
     * @param connectionManagerFactory
     * @param httpClientFactory
     */
    public SimpleHostRoutingWithProxyFilter(ProxyRequestHelper helper, ZuulProperties properties,
            ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
            ApacheHttpClientFactory httpClientFactory) {
        super(helper, properties, connectionManagerFactory, httpClientFactory);
    }

    /* (non-Javadoc)
     * @see org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter#newClient()
     */
    @Override
    protected CloseableHttpClient newClient() {
        if ((httpClient == null) || !(httpClient instanceof CloseableHttpClient)) {
            return super.newClient();
        } else {
            return (CloseableHttpClient) httpClient;
        }
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.getZuulRequestHeaders().put(HEADER_HOST, ctx.getRouteHost().getHost());
        RequestContext context = RequestContext.getCurrentContext();
        ConcurrentMap<String, List<String>> newParameterMap = new ConcurrentHashMap<>();

        if ((ctx.getRouteHost() != null) && (ctx.getRouteHost().getQuery() != null)) {
            String[] parameters = ctx.getRouteHost().getQuery().split("&");
            for (String parameter : parameters) {
                String[] keyValues = parameter.split("=");
                if (keyValues.length == 2) {
                    String authenticatedKey = keyValues[0];
                    String authenticatedValue = "";
                    if (keyValues[1] != null) {
                        authenticatedValue = keyValues[1];
                    }
                    if (newParameterMap.containsKey(keyValues[0])) {
                        newParameterMap.get(keyValues[0]).add(keyValues[1]);
                    }
                    newParameterMap.put(authenticatedKey, Lists.newArrayList(authenticatedValue));
                }
            }
            context.setRequestQueryParams(newParameterMap);
        }
        return super.run();
    }
}
