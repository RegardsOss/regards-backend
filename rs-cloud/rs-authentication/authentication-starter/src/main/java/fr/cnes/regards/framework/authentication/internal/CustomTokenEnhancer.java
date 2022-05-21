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
package fr.cnes.regards.framework.authentication.internal;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import java.util.Set;

/**
 * Class CustomTokenEnhancer
 * <p>
 * Add custom properties "CLAIMS" into each generated tokens
 *
 * @author Sébastien Binda
 */
public class CustomTokenEnhancer implements TokenEnhancer {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CustomTokenEnhancer.class);

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    public CustomTokenEnhancer(final JWTService pJwtService) {
        super();
        jwtService = pJwtService;
    }

    @Override
    public OAuth2AccessToken enhance(final OAuth2AccessToken pAccessToken, final OAuth2Authentication pAuthentication) {
        final UserDetails user = (UserDetails) pAuthentication.getUserAuthentication().getPrincipal();
        final Set<String> scopes = pAuthentication.getOAuth2Request().getScope();
        if ((jwtService != null) && (user != null) && (scopes != null) && !scopes.isEmpty()) {
            ((DefaultOAuth2AccessToken) pAccessToken).setAdditionalInformation(jwtService.generateClaims(scopes.stream()
                                                                                                               .findFirst()
                                                                                                               .get(),
                                                                                                         user.getRole(),
                                                                                                         user.getLogin(),
                                                                                                         user.getEmail()));
        } else {
            LOG.error("[OAUTH2 AUTHENTICATION] Error adding claims to JWT token.");
        }
        return pAccessToken;
    }
}
