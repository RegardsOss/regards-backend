/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fr.cnes.regards.microservices.core.configuration.cloud.CorsFilter;
import fr.cnes.regards.microservices.core.security.jwt.JWTAuthenticationFilter;

/**
 *
 * Enable Spring security for microservice
 *
 * @author msordi
 *
 */
@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    /**
     * Override security configuration to deny all requests unless user can be authenticated through a JWT.
     */
    @Override
    protected void configure(final HttpSecurity pHttp) throws Exception {
        pHttp.authorizeRequests()
                .antMatchers("/webjars/springfox-swagger-ui/**/*", "/swagger-resources", "/swagger-resources/**/*",
                             "/v2/**/*", "/swagger-ui.html", "/info")
                .permitAll().anyRequest().authenticated().and().csrf().disable()
                .addFilterBefore(new CorsFilter(), ChannelProcessingFilter.class)
                .addFilterBefore(new JWTAuthenticationFilter(authenticationManager()),
                                 UsernamePasswordAuthenticationFilter.class);

    }
}
