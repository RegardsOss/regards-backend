/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import fr.cnes.regards.framework.security.filter.CorsFilter;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Class AuthorizationServerConfiguration
 *
 * Spring Oauth2 Authorization server configuration. Configuration to authenticate users and get tokens.
 *
 * @author Sébastien Binda
 * @since 1.0-SNPASHOT
 */
public class Oauth2AuthorizationServerConfigurer extends AuthorizationServerConfigurerAdapter {

    /**
     * Resources identifier
     */
    private final String resourceId;

    /**
     * JWT Secret string to encrypt tokens
     */
    private final String jwtSecret;

    /**
     * Client login
     */
    private final String clientUser;

    /**
     * Client secret
     */
    private final String clientSecret;

    /**
     * Grant type
     */
    private final String grantType;

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    /**
     * OAuth2 authentication manager
     */
    private final AuthenticationManager authenticationManager;

    public Oauth2AuthorizationServerConfigurer(final String pResourceId, final String pJwtSecret,
            final String pClientUser, final String pClientSecret, final String pGrantType,
            final AuthenticationManager pAuthenticationManager, final JWTService pJwtService) {
        super();
        resourceId = pResourceId;
        jwtSecret = pJwtSecret;
        clientUser = pClientUser;
        clientSecret = pClientSecret;
        grantType = pGrantType;
        authenticationManager = pAuthenticationManager;
        jwtService = pJwtService;
    }

    /**
     *
     * Create custom token enhancer to add custom claims in the JWT token generated.
     *
     * @return CustomTokenEnhancer
     * @since 1.0-SNAPSHOT
     */
    private TokenEnhancer tokenEnhancer() {
        return new CustomTokenEnhancer(jwtService);
    }

    /**
     *
     * Create token store for spring JWT manager
     *
     * @return TokenStore
     * @since 1.0-SNAPSHOT
     */
    private TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    /**
     *
     * Create the Oauth2 token to JWT Token converter
     *
     * @return JwtAccessTokenConverter
     * @since 1.0-SNAPSHOT
     */
    private JwtAccessTokenConverter accessTokenConverter() {
        final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(jwtSecret);
        return converter;
    }

    @Override
    public void configure(final AuthorizationServerEndpointsConfigurer pEndpoints) throws Exception {
        final TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(), accessTokenConverter()));

        pEndpoints.tokenStore(tokenStore()).authenticationManager(this.authenticationManager)
                .accessTokenConverter(accessTokenConverter()).tokenEnhancer(tokenEnhancerChain);
    }

    @Override
    public void configure(final ClientDetailsServiceConfigurer pClients) throws Exception {
        pClients.inMemory().withClient(clientUser).authorizedGrantTypes(grantType).resourceIds(resourceId)
                .secret(clientSecret);
    }

    @Override
    public void configure(final AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.addTokenEndpointAuthenticationFilter(new CorsFilter());
    }

    /**
     *
     * Create token services
     *
     * @return DefaultTokenServices
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setSupportRefreshToken(true);
        return defaultTokenServices;
    }
}
