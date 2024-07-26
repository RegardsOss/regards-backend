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
package fr.cnes.regards.cloud.gateway.authentication;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.cloud.gateway.filters.FilterConstants.AUTHORIZATION;
import static fr.cnes.regards.cloud.gateway.filters.FilterConstants.BEARER;

public class ExternalAuthenticationVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalAuthenticationVerifier.class);

    private final WebClient.Builder webClientBuilder;

    private final JWTService jwtService;

    private final String appName;

    public ExternalAuthenticationVerifier(WebClient.Builder webClientBuilder, JWTService jwtService, String appName) {
        this.webClientBuilder = webClientBuilder;
        this.jwtService = jwtService;
        this.appName = appName;
    }

    public Mono<Authentication> verifyAndAuthenticate(String externalToken, String tenant) {
        String token = getSystemToken(tenant);
        return webClientBuilder.build()
                               .get()
                               .uri("http://rs-authentication/serviceproviders/verify?externalToken={externalToken}",
                                    externalToken)
                               .accept(MediaType.ALL)
                               .header(AUTHORIZATION, BEARER + " " + token)
                               .exchangeToMono(response -> {
                                   if (response.statusCode().equals(HttpStatus.OK)) {
                                       return response.bodyToMono(Authentication.class);
                                   } else {
                                       return response.createException().flatMap(Mono::error);
                                   }
                               });
    }

    private String getSystemToken(String tenant) {
        String role = RoleAuthority.getSysRole(appName);
        LOGGER.debug("Generating internal system JWT for application {}, tenant {} and role {} ",
                     appName,
                     tenant,
                     role);
        return jwtService.generateToken(tenant, appName, appName, role);
    }

}