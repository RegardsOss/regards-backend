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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.cnes.regards.cloud.gateway.authentication.ExternalAuthenticationVerifier;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.cloud.gateway.filters.FilterConstants.AUTHORIZATION;
import static fr.cnes.regards.cloud.gateway.filters.FilterConstants.BEARER;

/**
 * This filter detects invalid Regards JWT tokens, check if it is an allowed external token and tades it with a Regarfs token if so.<br/>
 * Target header : Bearer : Authorization
 *
 * @author Arnaud Bos
 */
public class ExternalTokenVerificationFilter implements GlobalFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTokenVerificationFilter.class);

    private final JWTService jwtService;

    private final ExternalAuthenticationVerifier externalAuthenticationVerifier;

    public ExternalTokenVerificationFilter(JWTService jwtService, ExternalAuthenticationVerifier externalAuthenticationVerifier) {
        this.jwtService = jwtService;
        this.externalAuthenticationVerifier = externalAuthenticationVerifier;
    }

    private final Cache<String, String> invalid = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();

    private final Cache<String, String> valid = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpConstants.AUTHORIZATION);

        if (!Strings.isNullOrEmpty(authHeader) && authHeader.startsWith(HttpConstants.BEARER)) {
            final String jwtKey = authHeader.substring(HttpConstants.BEARER.length()).trim();

            // if token invalid, no need to check,
            if (!invalid.asMap().containsKey(jwtKey)) {
                return tryAuthentication(request, jwtKey).flatMap(jwtVal -> {
                    ServerHttpRequest modifiedRequest = request.mutate().header(AUTHORIZATION, BEARER + " " + jwtVal).build();
                    // Even if it's an expired Regards token, pass it along, it will be invalidated by the JWTAuthenticationProvider down stream
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
            }
        }
        return chain.filter(exchange);
    }

    private Mono<String> tryAuthentication(ServerHttpRequest request, String jwtKey) {
        if (valid.asMap().containsKey(jwtKey)) {
            return Mono.just(Objects.requireNonNull(valid.getIfPresent(jwtKey)));
        } else {
            JWTAuthentication authentication = new JWTAuthentication(jwtKey);

            // Try to retrieve target tenant from request
            String tenant = request.getHeaders().getFirst(HttpConstants.SCOPE);
            if (Strings.isNullOrEmpty(tenant) && request.getQueryParams().containsKey(HttpConstants.SCOPE)) {
                tenant = request.getQueryParams().getFirst(HttpConstants.SCOPE);
            }
            if (!Strings.isNullOrEmpty(tenant)) {
                authentication.setTenant(tenant);
            }

            return Mono.fromCallable(() -> jwtService.parseToken(authentication))
                    .map(JWTAuthentication::getJwt)
                    .onErrorResume(JwtException.class, e ->
                                           externalAuthenticationVerifier.verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt())
                                           .map(Authentication::getAccessToken)
                                           .onErrorResume(t -> {
                                                LOGGER.info("Token verification failed (token={}).", jwtKey, t);
                                                // If not resolved, mark token as invalid.
                                                invalid.put(jwtKey, jwtKey);
                                                return Mono.empty();
                                           })
                                   )
                    // If resolved, it's supposed to be a valid REGARDS token in any case:
                    // 1) either because it was already a valid Regards token
                    // 2) or because it was a valid external token which was traded against a valid Regards token by the external "resolver".
                    // So we can cache it and pass along.
                    .map(regardsToken -> {
                        valid.put(jwtKey, regardsToken);
                        return regardsToken;
                    });
        }
    }

    @VisibleForTesting
    protected Cache<String, String> getInvalidCache() {
        return invalid;
    }

    @VisibleForTesting
    protected Cache<String, String> getValidCache() {
        return valid;
    }
}
