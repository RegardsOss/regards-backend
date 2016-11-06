/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;

/**
 *
 * Class AuthorizationServerConfiguration
 *
 * Spring Oauth2 Authorization server configuration. Configuration to authenticate users and get tokens.
 *
 * @author Sébastien Binda
 * @since 1.0-SNPASHOT
 */
@Configuration
@EnableAuthorizationServer
@EnableFeignClients(clients = { IAccountsClient.class, IProjectUsersClient.class })
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    /**
     * Resources identifier
     */
    @Value("${spring.application.name}")
    private String resourceId;

    /**
     * Validity time for token generated
     */
    @Value("${access_token.validity_period}")
    private int accessTokenValiditySeconds;

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

    /**
     * OAuth2 authentication manager
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     *
     * Create custom token enhancer to add custom claims in the JWT token generated.
     *
     * @return CustomTokenEnhancer
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public TokenEnhancer tokenEnhancer() {
        return new CustomTokenEnhancer();
    }

    /**
     *
     * Create token store for spring JWT manager
     *
     * @return TokenStore
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    /**
     *
     * Create the Oauth2 token to JWT Token converter
     *
     * @return JwtAccessTokenConverter
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
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
