/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.filter.IpFilter;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Class ResourceServerConfiguration
 *
 * Configuration to protect access to gateway resources. Check for the incoming JWT Token before allowing access to
 * resources
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    /**
     * Resources identifier
     */
    @Value("${spring.application.name}")
    private String resourceId;

    /**
     * Provider to access authorities resources and roles informations.
     */
    @Autowired
    private IAuthoritiesProvider authoritiesProvider;

    /**
     * Security service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * token services
     */
    @Autowired
    private DefaultTokenServices tokenServices;

    @Override
    public void configure(final ResourceServerSecurityConfigurer pResources) {
        pResources.resourceId(resourceId).tokenExtractor(new JwtTokenExtractor(jwtService)).tokenServices(tokenServices)
                .authenticationManager(new ResourceServerAuthenticationManager(jwtService));
    }

    @Override
    public void configure(final HttpSecurity pHttp) throws Exception {
        pHttp.requestMatcher(new OAuthRequestedMatcher()).authorizeRequests().antMatchers(HttpMethod.OPTIONS)
                .permitAll().anyRequest().authenticated();

        // Add Ip filter after Authentication filter
        pHttp.addFilterAfter(new IpFilter(authoritiesProvider), AbstractPreAuthenticatedProcessingFilter.class);

        // Add CORS filter
        pHttp.addFilterAfter(new RoleSysFilter(), IpFilter.class);
    }

    /**
     *
     * Class OAuthRequestedMatcher
     *
     * Determine if the client request contained an OAuth Authorization
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static class OAuthRequestedMatcher implements RequestMatcher {

        @Override
        public boolean matches(final HttpServletRequest pRequest) {
            final String auth = pRequest.getHeader("Authorization");
            final boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer");
            final boolean haveAccessToken = pRequest.getParameter("access_token") != null;
            return haveOauth2Token || haveAccessToken;
        }
    }

}