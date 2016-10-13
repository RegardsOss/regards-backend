/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fr.cnes.regards.framework.security.autoconfigure.filter.CorsFilter;
import fr.cnes.regards.framework.security.autoconfigure.filter.JWTAuthenticationFilter;

/**
 * @author msordi
 *
 */
@Configuration
@ConditionalOnWebApplication
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private CorsFilter cors;

    /**
     * Override security configuration to deny all requests unless user can be authenticated through a JWT.
     */
    @Override
    protected void configure(final HttpSecurity pHttp) throws Exception {
        pHttp.authorizeRequests()
                .antMatchers("/webjars/springfox-swagger-ui/**/*", "/swagger-resources", "/swagger-resources/**/*",
                             "/v2/**/*", "/swagger-ui.html", "/info")
                .permitAll().anyRequest().authenticated().and().csrf().disable()
                .addFilterBefore(cors, ChannelProcessingFilter.class)
                .addFilterBefore(new JWTAuthenticationFilter(authenticationManager()),
                                 UsernamePasswordAuthenticationFilter.class);

    }

}
