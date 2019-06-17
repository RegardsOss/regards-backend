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

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.springframework.util.Assert;

import fr.cnes.httpclient.HttpClient;

/**
 * @author sbinda
 *
 */
public class ProxyfiedHttpClient extends CloseableHttpClient {

    private final HttpClient client;

    public ProxyfiedHttpClient(HttpClient client) {
        super();
        this.client = client;
    }

    @Override
    @Deprecated
    public HttpParams getParams() {
        return this.client.getParams();
    }

    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager() {
        return this.client.getConnectionManager();
    }

    @Override
    public void close() throws IOException {
        this.client.close();

    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost host, HttpRequest request, HttpContext context)
            throws IOException, ClientProtocolException {
        Assert.state(request != null, "HttpRequest should not be null");
        HttpResponse response;

        if ((context != null) && (host != null)) {
            response = this.client.execute(host, request, context);
        } else if (host != null) {
            response = this.client.execute(host, request);
        } else {
            response = this.client.execute((HttpUriRequest) request);
        }
        return new ProxyfiedHttpResponse(response);
    }

}
