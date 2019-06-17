/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.proxy.ProxyfiedHttpClient;

/**
 * @author sbinda
 *
 */
@Component
@DependsOn("proxyHttpClient")
public class SimpleHostRoutingWithProxyFilter extends SimpleHostRoutingFilter {

    @Autowired
    private HttpClient httpClient;

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
        // TODO : Les paramètres ne sont pas reportés dans la requête proxyfiée !!!!!!!
    }

    /* (non-Javadoc)
     * @see org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter#newClient()
     */
    @Override
    protected CloseableHttpClient newClient() {
        if (httpClient == null) {
            return super.newClient();
        } else {
            return new ProxyfiedHttpClient((fr.cnes.httpclient.HttpClient) httpClient);
        }
    }

}
