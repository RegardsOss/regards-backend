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
package fr.cnes.regards.framework.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.httpclient.HttpClient;
import fr.cnes.httpclient.HttpClientFactory;
import fr.cnes.httpclient.HttpClientFactory.Type;

/**
 * Creates a {@link HttpClient} with proxy configuration.
 * Proxy configuration is set with spring properties with the possibilities of the JSPNego library.
 * This configuration is mained used for CNES specific proxy authorization.
 *
 * @see https://github.com/CNES/JSPNego
 * @author sbinda
 *
 */
@Configuration
public class ProxyConfiguration {

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port}:#{null}}")
    private Integer proxyPort;

    @Value(value = "${http.proxy.noproxy:#{null}}")
    private String noProxy;

    @Bean("proxyHttpClient")
    public HttpClient getHttpClient() {
        // https://github.com/CNES/JSPNego
        if (proxyHost != null) {
            fr.cnes.httpclient.configuration.ProxyConfiguration.HTTP_PROXY.setValue(proxyHost + ":" + proxyPort);
            if ((noProxy != null) && !noProxy.isEmpty()) {
                fr.cnes.httpclient.configuration.ProxyConfiguration.NO_PROXY.setValue(noProxy);
            }
            return HttpClientFactory.create(Type.PROXY_BASIC);
        } else {
            return HttpClientFactory.create(Type.NO_PROXY);
        }

    }

}
