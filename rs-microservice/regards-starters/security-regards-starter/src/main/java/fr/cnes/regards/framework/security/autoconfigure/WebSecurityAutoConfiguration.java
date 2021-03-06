/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;

import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.controller.SecurityResourcesController;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.filter.CorsFilter;
import fr.cnes.regards.framework.security.filter.IpFilter;
import fr.cnes.regards.framework.security.filter.JWTAuthenticationFilter;
import fr.cnes.regards.framework.security.filter.JWTAuthenticationProvider;
import fr.cnes.regards.framework.security.filter.PublicAuthenticationFilter;
import fr.cnes.regards.framework.security.filter.RequestLogFilter;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * Web security auto configuration
 * @author msordi
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityAutoConfiguration.class);

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Authorization service
     */
    @Autowired
    private MethodAuthorizationService authorizationService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Custom web security configuration
     */
    @Autowired(required = false)
    private Set<ICustomWebSecurityConfiguration> customConfigurers;

    /**
     * List of authorized ip for CORS request. If empty all origins are allowed. Split character ','
     */
    @Value("${regards.cors.requests.authorized.clients.addresses:#{null}}")
    private String corsRequestAuthorizedClientAddresses;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

        http.headers(headers -> headers.withObjectPostProcessor(new ObjectPostProcessor<HeaderWriterFilter>() {

            @Override
            public HeaderWriterFilter postProcess(HeaderWriterFilter headerWriterFilter) {
                headerWriterFilter.setShouldWriteHeadersEagerly(true);
                return headerWriterFilter;
            }
        }));
        // Disable spring security cache control to use a custom one forcing use of cache for specified endpoints.
        // This custom writter will be remove once spring-boo & jetty version is upgraded to fix issue https://github.com/spring-projects/spring-security/issues/9175
        // https://github.com/spring-projects/spring-security/issues/9175
        http.headers().cacheControl().disable();
        http.headers().addHeaderWriter(new CustomCacheControlHeadersWriter());
        // lets disable frame options by default and then add our writer that will set DENY by default and let any other
        // value if the devs choose one
        http.headers().frameOptions().disable();
        http.headers().addHeaderWriter(new XFrameOptionsHeaderWriterDefault());

        // Disable CSRF
        // Force authentication for all requests
        http.csrf().disable().authorizeRequests().anyRequest().authenticated();

        // Add public filter
        // TODO set in gateway
        http.addFilterBefore(new PublicAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        // Add JWT Authentication filter
        http.addFilterAfter(new JWTAuthenticationFilter(authenticationManager(), runtimeTenantResolver),
                            PublicAuthenticationFilter.class);
        http.addFilterBefore(new MDCInsertingServletFilter(), JWTAuthenticationFilter.class);
        http.addFilterAfter(new RequestLogFilter(), JWTAuthenticationFilter.class);

        // Add Ip filter after Authentication filter
        http.addFilterAfter(new IpFilter(authorizationService), JWTAuthenticationFilter.class);

        // Add CORS filter
        final List<String> authorizedIp = new ArrayList<>();
        if (corsRequestAuthorizedClientAddresses != null) {
            for (final String ip : corsRequestAuthorizedClientAddresses.split(",")) {
                if (ip.length() > 0) {
                    authorizedIp.add(ip);
                }
            }
        }
        http.addFilterAfter(new CorsFilter(authorizedIp), IpFilter.class);

        // Add custom configurations if any
        if (customConfigurers != null) {
            for (final ICustomWebSecurityConfiguration customConfigurer : customConfigurers) {
                customConfigurer.configure(http);
            }
        }

    }

    @Override
    public void configure(final WebSecurity pWeb) {
        pWeb.ignoring().antMatchers("/favicon", "/v3/**/*");
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
