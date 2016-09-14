/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@Configuration
@EnableResourceServer
public class OAuth2ResourceConfig extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {

        // Allow springfox swagger request to pass trough authentication system
        http.authorizeRequests().antMatchers("/webjars/springfox-swagger-ui/**/*", "/swagger-resources",
                                             "/swagger-resources/**/*", "/v2/**/*", "/swagger-ui.html")
                .permitAll().anyRequest().authenticated();
    }

}
