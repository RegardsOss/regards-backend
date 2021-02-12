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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import fr.cnes.regards.framework.authentication.internal.Oauth2AuthenticationManager;
import fr.cnes.regards.framework.authentication.internal.Oauth2AuthorizationServerConfigurer;
import fr.cnes.regards.framework.authentication.internal.Oauth2EndpointsConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.modules.authentication.plugins.identityprovider.regards.RegardsInternalAuthenticationPlugin;

/**
 * Class Oauth2AutoConfiguration
 *
 * Auto-configuration to activate Oauth2 authentication.
 * @author Sébastien Binda
 */
@Configuration
@EnableAuthorizationServer
@AutoConfigureAfter(MultitenantJpaAutoConfiguration.class)
public class Oauth2AutoConfiguration {

    /**
     * Resources identifier
     */
    @Value("${spring.application.name}")
    private String resourceId;

    /**
     * JWT Secret string to encrypt tokens
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Client login
     */
    @Value("${regards.authentication.client.user}")
    private String clientUser;

    /**
     * Client secret
     */
    @Value("${regards.authentication.client.secret}")
    private String clientSecret;

    /**
     * Grant type
     */
    @Value("${regards.authentication.granttype}")
    private String grantType;

    @Value("${access_token.validity_period:7200}")
    private Integer acessTokenValidityInSec;

    @Autowired
    private IRuntimeTenantResolver runTimeTenantResolver;

    @Autowired
    private JWTService jwtService;

    @Value("${regards.accounts.root.user.login}")
    private String rootUserLogin;

    @Bean
    public IAuthenticationPlugin defaultAuthenticationPlugin() {
        return new RegardsInternalAuthenticationPlugin();
    }

    /**
     * Create Authentication manager
     * @return Oauth2AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new Oauth2AuthenticationManager(defaultAuthenticationPlugin(), runTimeTenantResolver, rootUserLogin);
    }

    /**
     * Create Authorization server configurer
     * @return Oauth2AuthorizationServerConfigurer
     */
    @Bean
    public Oauth2AuthorizationServerConfigurer authorizationServer() {
        return new Oauth2AuthorizationServerConfigurer(resourceId, jwtSecret, clientUser, clientSecret, grantType,
                authenticationManager(), jwtService, acessTokenValidityInSec);
    }

    /**
     * Mvc specific configuration for Oauth2 endpoints
     * @return ICustomWebSecurityConfiguration
     */
    @Bean
    public ICustomWebSecurityConfiguration securityConf(AuthorizationServerEndpointsConfiguration endpoints) {
        return new Oauth2EndpointsConfiguration(endpoints);
    }

    /**
     * Create transactionManager mandatory by DefaultTokenService but we don't need it because JWT token are not
     * stored.<br/>
     * Set {@link PlatformTransactionManager} as primary to prevent conflict with another manager.
     * @return PlatformTransactionManager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManagerStub() {
        return new PlatformTransactionManager() {

            @Override
            public void rollback(TransactionStatus status) {
                // Nothing to do. The JwtTokenStore does not persists tokens
            }

            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                // Nothing to do. The JwtTokenStore does not persists tokens
                return null;
            }

            @Override
            public void commit(TransactionStatus status) {
                // Nothing to do. The JwtTokenStore does not persists tokens
            }
        };
    }

    /**
     * From now, password is given not encrypted
     */
    @Bean
    public static NoOpPasswordEncoder passwordEncoder() {
        return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
    }
}
