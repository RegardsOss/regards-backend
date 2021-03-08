/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.authentication.autoconfigure;

import feign.FeignException;
import fr.cnes.regards.framework.authentication.IExternalAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import io.vavr.control.Try;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.*;

public class ExternalAuthenticationResolver implements IExternalAuthenticationResolver {

    private IExternalAuthenticationClient externalAuthenticationClient;

    private IRuntimeTenantResolver runtimeTenantResolver;

    public ExternalAuthenticationResolver(IExternalAuthenticationClient externalAuthenticationClient, IRuntimeTenantResolver runtimeTenantResolver) {
        this.externalAuthenticationClient = externalAuthenticationClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public String verifyAndAuthenticate(String tenant, String externalToken) {
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
                String token = response.getBody();
                if (token == null) {
                    return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
                }
                return Try.success(token);
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
}
