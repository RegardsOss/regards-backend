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
 * @author sbinda
 *
 */
@Configuration
public class ProxyConfiguration {

    @Value("${http.proxy.host}")
    private String proxyHost;

    @Value("${http.proxy.port}")
    private int proxyPort;

    @Value(value = "${http.proxy.noproxy:#{null}}")
    private String noProxy;

    @Bean("proxyHttpClient")
    public HttpClient getHttpClient() {

        // Dependency : https://github.com/CNES/JSPNego
        // Pour compiler la doc : mvn site
        // Pour compiler la lib : mvn clean install
        // Attention, il faut désactiver les tests en positionnant à false toutes les variables skip tests dans le pom
        fr.cnes.httpclient.configuration.ProxyConfiguration.HTTP_PROXY.setValue(proxyHost + ":" + proxyPort);

        // Ici, ce sont les noms de domaines des noms de docker sur VMPERF lors des tests.
        if ((noProxy != null) && !noProxy.isEmpty()) {
            fr.cnes.httpclient.configuration.ProxyConfiguration.NO_PROXY.setValue(noProxy);
        }
        return HttpClientFactory.create(Type.PROXY_BASIC);
    }

}
