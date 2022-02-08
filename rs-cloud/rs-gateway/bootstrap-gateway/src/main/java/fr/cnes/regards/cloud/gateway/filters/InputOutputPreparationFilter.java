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

import ch.qos.logback.classic.ClassicConstants;
import fr.cnes.regards.framwork.logbackappender.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * This class is a proxy filter. It aims to log the HTTP method and the URL.</br>
 * It adds to the request header the X-Forwarded-For field.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public class InputOutputPreparationFilter implements GlobalFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputOutputPreparationFilter.class);

    private static final String LOG_PREFIX = "Inbound request (tracking id {}) : ";

    private static final String COMMA = ", ";

    public static final String CORRELATION_ID = "correlation-id";

    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest requestModified = alterHeaders(exchange.getRequest());
        logInput(requestModified);
        return chain.filter(exchange.mutate().request(requestModified).build())
                .then(Mono.fromRunnable(() -> logOutput(exchange)));
    }

    private ServerHttpRequest alterHeaders(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        HttpHeaders headersToAdd = new HttpHeaders();

        // Inject correlationId if not present in header
        if(!headers.containsKey(CORRELATION_ID)) {
            String generatedCorrelationId = generateCorrelationId();
            headersToAdd.add(CORRELATION_ID, generatedCorrelationId);
        }

        // Define xForwardedFor
        String xForwardedFor = headers.getFirst(X_FORWARDED_FOR);
        InetSocketAddress remoteInfo = Objects.requireNonNull(request.getRemoteAddress());
        String remoteAddr = remoteInfo.getAddress().getHostAddress();
        if(xForwardedFor != null) {
            if (xForwardedFor.isEmpty() && !xForwardedFor.contains(remoteAddr)) {
                xForwardedFor = xForwardedFor + COMMA + remoteAddr;
                headersToAdd.add(X_FORWARDED_FOR, xForwardedFor);
            } else {
                headersToAdd.add(X_FORWARDED_FOR, remoteAddr);
            }
        }

        return request.mutate().headers(headerAll -> {
            headerAll.addAll(headersToAdd);
            headerAll.remove("Cookie");
            headerAll.remove("Set-Cookie");
            headerAll.remove("Access-Control-Allow-Origin");
            headerAll.remove("Access-Control-Allow-Methods");
            headerAll.remove("Access-Control-Allow-Headers");
            headerAll.remove("Access-Control-Max-Age");
            headerAll.remove("Date");
        }).build();
    }


    private void logInput(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String correlationId = headers.getFirst(CORRELATION_ID);
        InetSocketAddress remoteInfo = Objects.requireNonNull(request.getRemoteAddress());
        String remoteAddr = remoteInfo.getAddress().getHostAddress();
        String remoteHost = remoteInfo.getHostName();
        String xForwardedFor = headers.getFirst(X_FORWARDED_FOR);
        URI requestURI = request.getURI();
        String requestMethod = Objects.requireNonNull(request.getMethod()).toString();
        String url = null;
        try {
            url =  requestURI.toURL().toString();
        } catch (MalformedURLException e) {
            LOGGER.error("Unable to get URL from request uri \"{}\"", requestURI);
        }

        LOGGER.info(LogConstants.SECURITY_MARKER + LOG_PREFIX + "{}@{} from {}", correlationId, requestURI, requestMethod, remoteAddr);
        MDC.put(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY, remoteHost);
        MDC.put(ClassicConstants.REQUEST_REQUEST_URI, requestURI.toString());
        MDC.put(ClassicConstants.REQUEST_REQUEST_URL, url);
        MDC.put(ClassicConstants.REQUEST_METHOD, requestMethod);
        MDC.put(ClassicConstants.REQUEST_QUERY_STRING, request.getQueryParams().toString());
        MDC.put(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY, headers.getFirst(HttpHeaders.USER_AGENT));
        MDC.put(ClassicConstants.REQUEST_X_FORWARDED_FOR, xForwardedFor);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(LOG_PREFIX + "Scheme: {}, Remote host: {}, Remote addr: {}, Remote port: {}, Remote user: {}, Header names: {}",
                    correlationId, requestURI.getScheme(), remoteHost, remoteAddr, remoteInfo.getPort(), request.getQueryParams(), headers.keySet());
            LOGGER.debug(LOG_PREFIX + "Forwarded headers => {}", correlationId, headers);
        }
    }

    private void logOutput(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Propagate correlation id
        String correlationId = req.getHeaders().getFirst(CORRELATION_ID);
        response.getHeaders().add(CORRELATION_ID, correlationId);

        LOGGER.info(LogConstants.SECURITY_MARKER + "Response ({}) for tracked request {} : {}@{} from {}",
                response.getStatusCode(),
                correlationId,
                req.getURI(),
                req.getMethod(),
                req.getRemoteAddress());
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
