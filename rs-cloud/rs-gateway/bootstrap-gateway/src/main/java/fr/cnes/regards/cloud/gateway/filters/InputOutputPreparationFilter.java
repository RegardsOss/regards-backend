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
import org.apache.commons.lang3.StringUtils;
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

import static fr.cnes.regards.cloud.gateway.filters.FilterConstants.*;

/**
 * This class is a proxy filter. It aims to log the HTTP method and the URL.</br>
 * It adds to the request header X_FORWARDED_FOR, CORRELATION_ID fields and if found JWT token the AUTHORIZATION field.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public class InputOutputPreparationFilter implements GlobalFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputOutputPreparationFilter.class);

    /**
     * Prefix log message
     */
    private static final String LOG_PREFIX = "Inbound request (tracking id {}) : ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest modifiedRequest = alterHeaders(exchange.getRequest());
        logInput(modifiedRequest);

        ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
        return chain.filter(modifiedExchange).then(Mono.fromRunnable(() -> {
            addCorrelationId(modifiedExchange);
            logOutput(modifiedExchange);
        }));
    }

    private ServerHttpRequest alterHeaders(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        HttpHeaders headersToAdd = new HttpHeaders();

        // Inject a new correlationId if not present in CORRELATION_ID header
        if (!headers.containsKey(CORRELATION_ID)) {
            headersToAdd.add(CORRELATION_ID, generateCorrelationId());
        }

        // Define X_FORWARDED_FOR header
        String xForwardedFor = headers.getFirst(X_FORWARDED_FOR);

        InetSocketAddress remoteInfo = Objects.requireNonNull(request.getRemoteAddress());
        String remoteAddress = remoteInfo.getAddress().getHostAddress();
        if (!StringUtils.isBlank(xForwardedFor) && !xForwardedFor.contains(remoteAddress)) {
            headersToAdd.add(X_FORWARDED_FOR, xForwardedFor + COMMA_WHITESPACE + remoteAddress);
        } else {
            headersToAdd.add(X_FORWARDED_FOR, remoteAddress);
        }

        // Try to retrieve JWT token in order to inject it in AUTHORIZATION header
        String jwt = request.getQueryParams().getFirst(TOKEN);
        // Inject into header if not null and bearer not already set
        if ((jwt != null) && (request.getQueryParams().getFirst(BEARER) == null)) {
            headersToAdd.add(AUTHORIZATION, BEARER + " " + jwt);
        }
        // Update list of headers of the request
        return request.mutate().headers(headerAll -> headerAll.addAll(headersToAdd)).build();
    }

    private void logInput(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        String correlationId = headers.getFirst(CORRELATION_ID);

        InetSocketAddress remoteInfo = Objects.requireNonNull(request.getRemoteAddress());
        String remoteAddress = remoteInfo.getAddress().getHostAddress();
        String remoteHost = remoteInfo.getHostName();

        String xForwardedFor = headers.getFirst(X_FORWARDED_FOR);
        URI requestURI = request.getURI();
        String requestMethod = Objects.requireNonNull(request.getMethod()).toString();

        LOGGER.info(LogConstants.SECURITY_MARKER + LOG_PREFIX + "{}@{} from {}",
                    correlationId,
                    requestURI,
                    requestMethod,
                    remoteAddress);

        String url = null;
        try {
            url = requestURI.toURL().toString();
        } catch (MalformedURLException e) {
            LOGGER.error("Unable to get URL from request uri \"{}\"", requestURI);
        }

        MDC.put(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY, remoteHost);
        MDC.put(ClassicConstants.REQUEST_REQUEST_URI, requestURI.toString());
        MDC.put(ClassicConstants.REQUEST_REQUEST_URL, url);
        MDC.put(ClassicConstants.REQUEST_METHOD, requestMethod);
        MDC.put(ClassicConstants.REQUEST_QUERY_STRING, request.getQueryParams().toString());
        MDC.put(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY, headers.getFirst(HttpHeaders.USER_AGENT));
        MDC.put(ClassicConstants.REQUEST_X_FORWARDED_FOR, xForwardedFor);

        LOGGER.debug(LOG_PREFIX
                     + "Scheme: {}, Remote host: {}, Remote addr: {}, Remote port: {}, Remote user: {}, Header names: {}",
                     correlationId,
                     requestURI.getScheme(),
                     remoteHost,
                     remoteAddress,
                     remoteInfo.getPort(),
                     request.getQueryParams(),
                     headers.keySet());

        LOGGER.debug(LOG_PREFIX + "Forwarded headers => {}", correlationId, headers);
    }

    /**
     * Propagate CORRELATION_ID header in list of headers of response from the request
     */
    private void addCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        exchange.getResponse().getHeaders().add(CORRELATION_ID, correlationId);
    }

    private void logOutput(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        LOGGER.info(LogConstants.SECURITY_MARKER + "Response ({}) for tracked request {} : {}@{} from {}",
                    response.getStatusCode(),
                    response.getHeaders().getFirst(CORRELATION_ID),
                    request.getURI(),
                    request.getMethod(),
                    request.getRemoteAddress());
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
