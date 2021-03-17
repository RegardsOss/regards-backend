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
package fr.cnes.regards.cloud.gateway.filters;

//@Component
public class ExternalAuthenticationResolver {// implements IExternalAuthenticationResolver {

//    private IRuntimeTenantResolver runtimeTenantResolver;
//
//    private IExternalAuthenticationClient externalAuthenticationClient;
//
//    @Autowired
//    public ExternalAuthenticationResolver(IRuntimeTenantResolver runtimeTenantResolver, IExternalAuthenticationClient externalAuthenticationClient) {
//        this.runtimeTenantResolver = runtimeTenantResolver;
//        this.externalAuthenticationClient = externalAuthenticationClient;
//    }
//
//    @Override
//    public String verifyAndAuthenticate(String tenant, String externalToken) {
//        return Try.run(() -> {
//            FeignSecurityManager.asSystem();
//            runtimeTenantResolver.forceTenant(tenant);
//        })
//            .map(ignored -> externalAuthenticationClient.verifyAndAuthenticate(externalToken))
//            .transform(this::mapClientException)
//            .flatMap(response -> {
//                if (response.getStatusCode() != HttpStatus.OK) {
//                    return Try.failure(new InsufficientAuthenticationException(String.format("Service Provider rejected userInfo request with status: %s", response.getStatusCode())));
//                }
//                Authentication auth = response.getBody();
//                if (auth == null) {
//                    return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
//                }
//                return Try.success(auth.getAccessToken());
//            })
//            .andFinally(() -> {
//                runtimeTenantResolver.clearTenant();
//                FeignSecurityManager.reset();
//            })
//            .get();
//    }
//
//    private <T> Try<T> mapClientException(Try<T> call) {
//        //noinspection unchecked
//        return call.mapFailure(
//            Case($(instanceOf(HttpClientErrorException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex)),
//            Case($(instanceOf(HttpServerErrorException.class)), ex -> new AuthenticationServiceException(ex.getMessage(), ex)),
//            Case($(instanceOf(FeignException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex))
//        );
//    }
}
