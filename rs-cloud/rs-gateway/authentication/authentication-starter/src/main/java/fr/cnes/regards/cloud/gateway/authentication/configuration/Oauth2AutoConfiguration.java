/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.impl.regards.RegardsInternalAuthenticationPlugin;
import fr.cnes.regards.framework.authentication.internal.Oauth2AuthenticationManager;
import fr.cnes.regards.framework.authentication.internal.Oauth2AuthorizationServerConfigurer;
import fr.cnes.regards.framework.authentication.internal.Oauth2EndpointsConfiguration;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Class Oauth2AutoConfiguration
 *
 * Auto-configuration to activate Oauth2 authentication.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableAuthorizationServer
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

    @Autowired
    private JWTService jwtService;

    @Bean
    public IAuthenticationPlugin defaultAuthenticationPlugin() {
        return new RegardsInternalAuthenticationPlugin();
    }

    /**
     *
     * Create Authentication manager
     *
     * @return Oauth2AuthenticationManager
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new Oauth2AuthenticationManager(resourceId, defaultAuthenticationPlugin(), jwtService);
    }

    /**
     *
     * Create Authorization server configurer
     *
     * @return Oauth2AuthorizationServerConfigurer
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public Oauth2AuthorizationServerConfigurer authorizationServer() {
        return new Oauth2AuthorizationServerConfigurer(resourceId, jwtSecret, clientUser, clientSecret, grantType,
                authenticationManager(), jwtService);
    }

    /**
     *
     * Mvc specific configuration for Oauth2 endpoints
     *
     * @return ICustomWebSecurityConfiguration
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ICustomWebSecurityConfiguration securityConf(final AuthorizationServerEndpointsConfiguration pEndpoints) {
        return new Oauth2EndpointsConfiguration(pEndpoints);
    }

    /**
     *
     * Create transactionManager mandatory by DefaultTokenService but we dont need it because JWT token are not stored.
     *
     * @return PlatformTransactionManager
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManagerStub() {
        return new PlatformTransactionManager() {

            @Override
            public void rollback(final TransactionStatus pStatus) {
                // Nothing to do. The JwtTokenStore does not persists tokens
            }

            @Override
            public TransactionStatus getTransaction(final TransactionDefinition pDefinition) {
                // Nothing to do. The JwtTokenStore does not persists tokens
                return null;
            }

            @Override
            public void commit(final TransactionStatus pStatus) {
                // Nothing to do. The JwtTokenStore does not persists tokens
            }
        };
    }

}
