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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.proxy;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * This configuration creates httpClient bean. If http.proxy.enabled is true, ProxyConfiguration override this httpClient bean.
 *
 * @author tguillou
 */
@Configuration
public class NoProxyConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoProxyConfiguration.class);

    @Value("${http.ssl.allow_insecure:false}")
    private boolean allowInsecure;

    /**
     * Copy of OpenIdConnectPlugin::getHttpClient, without proxies steps.
     */
    @Bean
    public HttpClient getHttpClient() {
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
        HttpClientBuilder builder = HttpClients.custom();
        SSLContext unChecksslContext = null;
        SSLConnectionSocketFactory uncheckSslConnectionSocketFactory = null;
        Registry<ConnectionSocketFactory> sslUncheckedSocketFactoryRegistry = null;
        if (allowInsecure) {
            LOGGER.info("#################################################");
            LOGGER.info("#### REGARDS HTTP Client unsecured created ######");
            LOGGER.info("#################################################");
            try {
                unChecksslContext = SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
                uncheckSslConnectionSocketFactory = new SSLConnectionSocketFactory(unChecksslContext,
                                                                                   NoopHostnameVerifier.INSTANCE);
                sslUncheckedSocketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                                                                   .register("https", uncheckSslConnectionSocketFactory)
                                                                   .build();
            } catch (Exception e) {
                LOGGER.error("Error creating SSL Context unchecked", e);
            }
        } else {
            LOGGER.info("#######################################");
            LOGGER.info("#### REGARDS HTTP Client created ######");
            LOGGER.info("#######################################");
        }
        PoolingHttpClientConnectionManager connManager;
        if (sslUncheckedSocketFactoryRegistry != null) {
            connManager = new PoolingHttpClientConnectionManager(sslUncheckedSocketFactoryRegistry);
        } else {
            connManager = new PoolingHttpClientConnectionManager();
        }
        connManager.setDefaultMaxPerRoute(10);
        connManager.setMaxTotal(20);
        builder.setConnectionManager(connManager).setKeepAliveStrategy((httpResponse, httpContext) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(httpResponse.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 5 * 1000;
        });

        if (unChecksslContext != null && uncheckSslConnectionSocketFactory != null) {
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            builder.setSSLContext(unChecksslContext);
            builder.setSSLSocketFactory(uncheckSslConnectionSocketFactory);
        }

        return builder.build();
    }
}
