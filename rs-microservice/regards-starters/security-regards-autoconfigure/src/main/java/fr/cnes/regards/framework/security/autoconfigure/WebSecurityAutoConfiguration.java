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
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fr.cnes.regards.framework.security.autoconfigure.filter.CorsFilter;
import fr.cnes.regards.framework.security.autoconfigure.filter.JWTAuthenticationFilter;
import fr.cnes.regards.framework.security.autoconfigure.filter.JWTAuthenticationProvider;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    @Override
    protected void configure(HttpSecurity pHttp) throws Exception {

        // Disable CSRF
        // Force authentication for all requests
        pHttp.csrf().disable().authorizeRequests().anyRequest().authenticated();

        // Add CORS filter
        pHttp.addFilterBefore(new CorsFilter(), ChannelProcessingFilter.class);

        // Add JWT Authentication filter
        pHttp.addFilterBefore(new JWTAuthenticationFilter(authenticationManager()),
                              UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public void configure(WebSecurity pWeb) throws Exception {
        pWeb.ignoring().antMatchers("/favicon", "/webjars/springfox-swagger-ui/**/*", "/swagger-resources",
                                    "/swagger-resources/**/*", "/v2/**/*", "/swagger-ui.html");
    }

    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider(jwtService);
    }

}
