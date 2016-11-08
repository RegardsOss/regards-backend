/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.controller.SecurityResourcesController;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.filter.CorsFilter;
import fr.cnes.regards.framework.security.filter.IpFilter;
import fr.cnes.regards.framework.security.filter.JWTAuthenticationFilter;
import fr.cnes.regards.framework.security.filter.JWTAuthenticationProvider;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Web security auto configuration
 *
 * @author msordi
 *
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Authorities provider
     */
    @Autowired
    private IAuthoritiesProvider authoritiesProvider;

    /**
     * Authorization service
     */
    @Autowired
    private MethodAuthorizationService authorizationService;

    /**
     * Custom web security configuration
     */
    @Autowired(required = false)
    private ICustomWebSecurityConfiguration customConfigurer;

    @Override
    protected void configure(final HttpSecurity pHttp) throws Exception {

        // Disable CSRF
        // Force authentication for all requests
        pHttp.csrf().disable().authorizeRequests().anyRequest().authenticated();

        // Add JWT Authentication filter
        pHttp.addFilterBefore(new JWTAuthenticationFilter(authenticationManager()),
                              UsernamePasswordAuthenticationFilter.class);

        // Add Ip filter after Authentication filter
        pHttp.addFilterAfter(new IpFilter(authoritiesProvider), JWTAuthenticationFilter.class);

        // Add CORS filter
        pHttp.addFilterAfter(new CorsFilter(authoritiesProvider), IpFilter.class);

        // Add custom configuration if any
        if (customConfigurer != null) {
            customConfigurer.configure(pHttp);
        }

    }

    @Override
    public void configure(final WebSecurity pWeb) throws Exception {
        pWeb.ignoring().antMatchers("/favicon", "/webjars/springfox-swagger-ui/**/*", "/swagger-resources",
                                    "/swagger-resources/**/*", "/v2/**/*", "/swagger-ui.html");
    }

    @Bean
    public SecurityResourcesController securityController() {
        return new SecurityResourcesController(authorizationService);
    }

    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider(jwtService);
    }

}
