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
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * This filter detects invalid Regards JWT tokens, check if it is an allowed external token and tades it with a Regarfs token if so.<br/>
 * Target header : Bearer : Authorization
 */
@Component
public class ExternalTokenVerificationFilter extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalTokenVerificationFilter.class);

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Authorization header scheme
     */
    public static final String BEARER = "Bearer";

    protected static final int ORDER = UrlToHeaderTokenFilter.ORDER + 1;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IExternalAuthenticationClient externalAuthenticationClient;

    @VisibleForTesting
    public ExternalTokenVerificationFilter(JWTService jwtService, IRuntimeTenantResolver runtimeTenantResolver, IExternalAuthenticationClient externalAuthenticationClient) {
        this.jwtService = jwtService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.externalAuthenticationClient = externalAuthenticationClient;
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
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        filter(ctx, request);

        return null;
    }

    @VisibleForTesting
    protected void filter(RequestContext ctx, HttpServletRequest request) {
        String authHeader = request.getHeader(HttpConstants.AUTHORIZATION);

        if (!Strings.isNullOrEmpty(authHeader) && authHeader.startsWith(HttpConstants.BEARER)) {
            final String jwtKey = authHeader.substring(HttpConstants.BEARER.length()).trim();

            // if token invalid, no need to check, just exit
            if (invalid.asMap().containsKey(jwtKey)) {
                return;
            }

            // if token valid, return its mapped value
            if (valid.asMap().containsKey(jwtKey)) {
                String jwtVal = valid.getIfPresent(jwtKey);
                ctx.getZuulRequestHeaders().put(AUTHORIZATION, BEARER + " " + jwtVal);
                // Even if it's an expired Regards token, pass it along, it will be invalidated by the JWTAuthenticationProvider down stream
                return;
            }

            JWTAuthentication authentication = new JWTAuthentication(jwtKey);

            // Try to retrieve target tenant from request
            String tenant = request.getHeader(HttpConstants.SCOPE);
            if (Strings.isNullOrEmpty(tenant) && request.getParameter(HttpConstants.SCOPE) != null) {
                tenant = request.getParameter(HttpConstants.SCOPE);
            }
            if (!Strings.isNullOrEmpty(tenant)) {
                authentication.setTenant(tenant);
            }

            Try.of(() -> jwtService.parseToken(authentication))
                .map(JWTAuthentication::getJwt)
                .recoverWith(JwtException.class, e -> Try
                    // If not a REGARDS token, let's try to resolve an external token.
                    .of(() -> verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt()))
                    .onFailure(t -> LOG.info("Token verification failed (token={}).", jwtKey, t))
                )
                // If not resolved, mark token as invalid.
                .onFailure(t -> invalid.put(jwtKey, jwtKey))
                // If resolved, it's supposed to be a valid REGARDS token in any case:
                // 1) either because it was already a valid Regards token
                // 2) or because it was a valid external token which was traded against a valid Regards token by the external "resolver".
                // So we can cache it and pass along.
                .peek(regardsToken -> valid.put(jwtKey, regardsToken))
                .andThen(regardsToken -> ctx.getZuulRequestHeaders().put(AUTHORIZATION, BEARER + " " + regardsToken));
        }
    }

    @VisibleForTesting
    protected String verifyAndAuthenticate(String tenant, String externalToken) {
        return Try.run(() -> {
            FeignSecurityManager.asSystem();
            runtimeTenantResolver.forceTenant(tenant);
        })
            .map(ignored -> externalAuthenticationClient.verifyAndAuthenticate(externalToken))
            .transform(this::mapClientException)
            .flatMap(response -> {
                if (response.getStatusCode() != HttpStatus.OK) {
                    return Try.failure(new InsufficientAuthenticationException(String.format("Service Provider rejected userInfo request with status: %s", response.getStatusCode())));
                }
                Authentication auth = response.getBody();
                if (auth == null) {
                    return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
                }
                return Try.success(auth.getAccessToken());
            })
            .andFinally(() -> {
                runtimeTenantResolver.clearTenant();
                FeignSecurityManager.reset();
            })
            .get();
    }

    private <T> Try<T> mapClientException(Try<T> call) {
        //noinspection unchecked
        return call.mapFailure(
            Case($(instanceOf(HttpClientErrorException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex)),
            Case($(instanceOf(HttpServerErrorException.class)), ex -> new AuthenticationServiceException(ex.getMessage(), ex)),
            Case($(instanceOf(FeignException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex))
        );
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
