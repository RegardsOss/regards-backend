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

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * This filter detects JWT in the URL query params and if found set it into the header.<br/>
 * Source URL query param : token<br/>
 * Target header : Bearer : Authorization
 *
 * @author Marc Sordi
 */
public class UrlToHeaderTokenFilter implements GlobalFilter {

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Authorization header scheme
     */
    public static final String BEARER = "Bearer";

    /**
     * Token that may be passed through request query parameter for download purpose
     */
    public static final String TOKEN = "token";


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Try to retrieve JWT
        ServerHttpRequest request = exchange.getRequest();
        String jwt = request.getQueryParams().getFirst(TOKEN);
        // Inject into header if not null and bearer not already set
        if ((jwt != null) && (request.getQueryParams().getFirst(BEARER) == null)) {
            ServerHttpRequest requestModified = request.mutate().header(AUTHORIZATION, BEARER + " " + jwt).build();
            return chain.filter(exchange.mutate().request(requestModified).build());
        } else {
            return chain.filter(exchange);
        }
    }
}
