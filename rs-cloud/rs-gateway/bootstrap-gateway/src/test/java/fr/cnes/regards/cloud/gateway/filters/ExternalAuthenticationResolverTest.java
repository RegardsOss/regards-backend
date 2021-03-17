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

public class ExternalAuthenticationResolverTest {

//    @Mock
//    private IExternalAuthenticationClient externalAuthenticationClient;
//
//    @Mock
//    private IRuntimeTenantResolver runtimeTenantResolver;
//
//    private IExternalAuthenticationResolver resolver;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        resolver = new ExternalAuthenticationResolver(runtimeTenantResolver, externalAuthenticationClient);
//    }
//
//    @Test
//    public void verify_fail_when_client_fails() {
//        HttpClientErrorException: {
//            HttpClientErrorException expected = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
//            doThrow(expected)
//                .when(externalAuthenticationClient)
//                .verifyAndAuthenticate(anyString());
//            assertThatThrownBy(() -> resolver.verifyAndAuthenticate("plop", "plop"))
//                .isExactlyInstanceOf(InternalAuthenticationServiceException.class)
//                .hasCauseReference(expected);
//        }
//
//        HttpServerErrorException: {
//            HttpServerErrorException expected = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
//            doThrow(expected)
//                .when(externalAuthenticationClient)
//                .verifyAndAuthenticate(anyString());
//            assertThatThrownBy(() -> resolver.verifyAndAuthenticate("plop", "plop"))
//                .isExactlyInstanceOf(AuthenticationServiceException.class)
//                .hasCauseReference(expected);
//        }
//
//        FeignException: {
//            FeignException expected = mock(FeignException.class);
//            doThrow(expected)
//                .when(externalAuthenticationClient)
//                .verifyAndAuthenticate(anyString());
//            assertThatThrownBy(() -> resolver.verifyAndAuthenticate("plop", "plop"))
//                .isExactlyInstanceOf(InternalAuthenticationServiceException.class)
//                .hasCauseReference(expected);
//        }
//    }
//
//    @Test
//    public void verify_fail_when_server_returns_unexpected_status_code() {
//        doReturn(ResponseEntity.noContent().build())
//            .when(externalAuthenticationClient)
//            .verifyAndAuthenticate(anyString());
//        assertThatThrownBy(() -> resolver.verifyAndAuthenticate("plop", "plop"))
//            .isExactlyInstanceOf(InsufficientAuthenticationException.class);
//    }
}
