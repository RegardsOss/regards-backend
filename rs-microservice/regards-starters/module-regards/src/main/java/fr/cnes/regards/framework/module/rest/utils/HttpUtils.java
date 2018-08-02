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
package fr.cnes.regards.framework.module.rest.utils;

import com.google.common.net.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public final class HttpUtils {

    /**
     * Http code class multiplier
     */
    private static final int HTTP_CODE_CLASS_MULTIPLIER = 100;

    /**
     * Not standard HTTP usual code
     */
    public static final int UNKNOWN_ERROR = 520;

    private HttpUtils() {
        // private constructor of a util class
    }

    /**
     * check {https://tools.ietf.org/html/rfc7231#section-6} for information
     */
    public static boolean isSuccess(HttpStatus pHttpStatus) {
        return (pHttpStatus.value() / HTTP_CODE_CLASS_MULTIPLIER) == 2;
    }


    /**
     * Build the public URL of the given endpoint by extracting headers from an user request
     * @param request      Request receive by a REST endpoint
     * @param endpointPath the endpoint you want to get the link
     * @param queryParams  Params to add after endpointPath
     * @return A public URL (with the gateway address instead of this µService adress) that user can contact
     */
    public static URI retrievePublicURI(HttpServletRequest request, String endpointPath, String queryParams) throws MalformedURLException, URISyntaxException {
        // Save the current URL
        URL url = new URL(request.getRequestURL().toString());
        String userInfo = url.getUserInfo();
        String scheme = url.getProtocol();

        String host;
        int port;
        // Here is the same implementation than spring-hateoas.
        // see ControllerLinkBuilder.java for additional infos
        String headerXForwardedHost = request.getHeader(HttpHeaders.X_FORWARDED_HOST);
        if (headerXForwardedHost != null) {
            // Handle several IP separated by a comma
            if (headerXForwardedHost.contains(",")) {
                String[] headerSplit = headerXForwardedHost.split(",");
                host = retrieveHostFromHeader(headerSplit[0]);
                // handle port extraction from this header
                port = retrievePortFromHeader(headerSplit[0]);
            } else {
                host = retrieveHostFromHeader(headerXForwardedHost);
                // handle port extraction from this header
                port = retrievePortFromHeader(headerXForwardedHost);
            }
        } else {
            // Fallback to the original request
            host = url.getHost();
            port = url.getPort();
        }
        // Check if the header with the port is provided
        String portHeader = request.getHeader(HttpHeaders.X_FORWARDED_PORT);
        if (portHeader != null && !portHeader.isEmpty()) {
            port = Integer.parseInt(portHeader);
        }

        // Check if the protocol is provided
        String sslHeader = request.getHeader("X-Forwarded-Ssl");
        String protoHeader = request.getHeader(HttpHeaders.X_FORWARDED_PROTO);

        if (protoHeader != null && !protoHeader.isEmpty()) {
            scheme = protoHeader;
        } else if (sslHeader != null && sslHeader.equalsIgnoreCase("on")) {
            scheme = "https";
        }

        // Create the URL
        return new URI(scheme, userInfo, host, port, endpointPath, queryParams, null);
    }

    private static int retrievePortFromHeader(String header) {
        int port;
        if (header.contains(":")) {
            String[] hostAndPort = header.split(":");
            port = Integer.parseInt(hostAndPort[1]);
        } else {
            port = 80;
        }
        return port;
    }

    private static String retrieveHostFromHeader(String header) {
        String host;
        if (header.contains(":")) {
            String[] hostAndPort = header.split(":");
            host = hostAndPort[0];
        } else {
            host = header;
        }
        return host;
    }
}
