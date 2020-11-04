/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import fr.cnes.httpclient.HttpClientFactory;

/**
 * Creates a {@link HttpClient} with proxy configuration.
 * Proxy configuration is set with spring properties with the possibilities of the JSPNego library.
 * This configuration is mained used for CNES specific proxy authorization.
 *
 * @see "https://github.com/CNES/JSPNego"
 * @author sbinda
 *
 */
@Configuration
public class ProxyConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfiguration.class);

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.login:#{null}}")
    private String proxyLogin;

    @Value("${http.proxy.password:#{null}}")
    private String proxyPassword;

    @Value("${http.proxy.port:#{null}}")
    private Integer proxyPort;

    @Value("${http.proxy.noproxy:#{T(java.util.Collections).emptyList()}}")
    private List<String> noProxy;

    @Bean("proxyHttpClient")
    @Primary
    public HttpClient getHttpClient() {
        LOGGER.info("####################################");
        LOGGER.info("#### REGARDS HTTP Proxy enabled ####");
        LOGGER.info("####################################");
        //  Kept in case there is some server that do not support being ask to create a TLSv1 connection while we can also speak in TLSv1.2...
        //  This allows use to force the usage of only TLSv1.2
        //  You just need to add a call to HttpClientBuilder#setSSLSocketFactor(sslsf) to activate it
        //        // specify some SSL parameter for clients only
        //        SSLContext sslcontext = SSLContexts.createDefault();
        //        // Allow TLSv1.2 protocol only
        //        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,
        //                                                                          new String[] { "TLSv1.2" },
        //                                                                          null,
        //                                                                          SSLConnectionSocketFactory
        //                                                                                  .getDefaultHostnameVerifier());
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if ((proxyHost != null) && !proxyHost.isEmpty()) {
            HttpClientBuilder builder = HttpClientBuilder.create();
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            if (((proxyLogin != null) && !proxyLogin.isEmpty()) && ((proxyPassword != null) && !proxyPassword
                    .isEmpty())) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                                             new UsernamePasswordCredentials(proxyLogin, proxyPassword));
                builder.setDefaultCredentialsProvider(credsProvider);
            }
            if (noProxy != null) {
                HttpRoutePlanner routePlannerHandlingNoProxy = new DefaultProxyRoutePlanner(proxy) {

                    @Override
                    public HttpRoute determineRoute(final HttpHost host, final HttpRequest request,
                            final HttpContext context) throws HttpException {
                        String hostname = host.getHostName();
                        if (noProxy.contains(hostname)) {
                            // Return direct route
                            return new HttpRoute(host);
                        }
                        return super.determineRoute(host, request, context);
                    }
                };
                return httpClientBuilder.setProxy(proxy).setRoutePlanner(routePlannerHandlingNoProxy).build();
            }
            return httpClientBuilder.setProxy(proxy).build();
        } else {
            return httpClientBuilder.build();
        }
    }

    /**
     * Method to use jspNego CNES library to create an httpclient whish can connect throught kerberos auth proxy
     * Not used anymore as it leads to http connections never closed.
     * @return
     */
    private HttpClient buildJspNegoClient() {
        // https://github.com/CNES/JSPNego
        if ((proxyHost != null) && !proxyHost.isEmpty()) {
            LOGGER.info("HTTP Proxy initialized with values host={}, port={},login={}",
                        proxyHost,
                        proxyPort,
                        proxyLogin);
            if (proxyPort != null) {
                fr.cnes.httpclient.configuration.ProxyConfiguration.HTTP_PROXY.setValue(proxyHost + ":" + proxyPort);
            } else {
                fr.cnes.httpclient.configuration.ProxyConfiguration.HTTP_PROXY.setValue(proxyHost);
            }
            if ((noProxy != null) && !noProxy.isEmpty()) {
                fr.cnes.httpclient.configuration.ProxyConfiguration.NO_PROXY
                        .setValue(noProxy.stream().collect(Collectors.joining(",")));
            }
            if ((proxyLogin != null) && (proxyPassword != null)) {
                fr.cnes.httpclient.configuration.ProxyConfiguration.USERNAME.setValue(proxyLogin);
                fr.cnes.httpclient.configuration.ProxyConfiguration.PASSWORD.setValue(proxyPassword);
            }

            return HttpClientFactory.create(HttpClientFactory.Type.PROXY_BASIC);
        } else {
            LOGGER.info("No HTTP Proxy configured.");
            return HttpClientFactory.create(HttpClientFactory.Type.NO_PROXY);
        }
    }

}
