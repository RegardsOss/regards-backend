package fr.cs.regards;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * 
 * This class allow OPTIONS Request without oauth2 authentication.
 * The options requests are used by CORS requests to check if 
 * cross origins requests are allowed by server 
 * 
 * @author sbinda
 *
 */
@Configuration
@Order(-1)
public class CorsConfiguration extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.requestMatchers().antMatchers(HttpMethod.OPTIONS, "/**")
            .and()
                .csrf().disable()
            .authorizeRequests().anyRequest().permitAll()
            .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}


