/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 *
 * Class AuthorizationServerConfiguration
 *
 * Spring Oauth2 Authorization server configuration. Configuration to authenticate users and get tokens.
 *
 * @author CS
 * @since 1.0-SNPASHOT
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    /**
     * Resources identifier
     */
    @Value("${spring.application.name}")
    private String resourceId_;

    /**
     * Validity time for token generated
     */
    @Value("${access_token.validity_period}")
    private int accessTokenValiditySeconds_;

    /**
     * JWT Secret string to encrypt tokens
     */
    @Value("${jwt.secret}")
    private String jwtSecret_;

    /**
     * Client login
     */
    @Value("${authentication.client.user}")
    private String clientUser_;

    /**
     * Client secret
     */
    @Value("${authentication.client.secret}")
    private String clientSecret_;

    /**
     * Grant type
     */
    @Value("${authentication.granttype}")
    private String grantType_;

    /**
     * OAuth2 authentication manager
     */
    @Autowired
    private AuthenticationManager authenticationManager_;

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
        converter.setSigningKey(jwtSecret_);
        return converter;
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer pEndpoints) throws Exception {

        final TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(), accessTokenConverter()));

        pEndpoints.tokenStore(tokenStore()).authenticationManager(this.authenticationManager_)
                .accessTokenConverter(accessTokenConverter()).tokenEnhancer(tokenEnhancerChain);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer pClients) throws Exception {
        pClients.inMemory().withClient(clientUser_).authorizedGrantTypes(grantType_).resourceIds(resourceId_)
                .secret(clientSecret_);
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
