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

import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityAuthorizeRequestsConfiguration;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityFilterConfiguration;
import fr.cnes.regards.framework.security.controller.SecurityResourcesController;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.filter.*;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Web security auto configuration
 *
 * @author msordi
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 */
@AutoConfiguration
@EnableWebSecurity
@ConditionalOnWebApplication
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebSecurityAutoConfiguration {

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
    private Set<ICustomWebSecurityAuthorizeRequestsConfiguration> customAuthorizeRequestsConfigurers;

    /**
     * Custom web security configuration
     */
    @Autowired(required = false)
    private Set<ICustomWebSecurityFilterConfiguration> customFilterConfigurers;

    /**
     * List of authorized ip for CORS request. If empty all origins are allowed. Split character ','
     */
    @Value("${regards.cors.requests.authorized.clients.addresses:#{null}}")
    private String corsRequestAuthorizedClientAddresses;

    /**
     * Client user (only needed by rs-authentication through BasicAuthenticationFilter)
     * Not null value set only to avoid initialisation to fail
     */
    @Value("${regards.authentication.client.user:only_needed_by_rs-authentication}")
    private String clientUser;

    /**
     * Client secret (only needed by rs-authentication through BasicAuthenticationFilter)
     * Not null value set only to avoid initialisation to fail
     */
    @Value("${regards.authentication.client.secret:only_needed_by_rs-authentication}")
    private String clientSecret;

    /**
     * List of routes that must not use JWTAuthenticationFilter
     */
    private Set<String> noSecurityRoutes = Sets.newHashSet("/favicon", "/v3/**", "/v3/api-docs", "/swagger-ui/**");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JWTAuthenticationManager authenticationManager)
        throws Exception {
        http.headers(headers -> headers //
                                        .withObjectPostProcessor(new ObjectPostProcessor<HeaderWriterFilter>() {

                                            @Override
                                            @SuppressWarnings("unchecked")
                                            public HeaderWriterFilter postProcess(HeaderWriterFilter headerWriterFilter) {
                                                headerWriterFilter.setShouldWriteHeadersEagerly(true);
                                                return headerWriterFilter;
                                            }
                                        }) //
                                        // Disable spring security cache control to use a custom one forcing use of cache for specified endpoints.
                                        .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                                        // This custom writter will be remove once spring-boot & jetty version is upgraded to fix issue
                                        // https://github.com/spring-projects/spring-security/issues/9175
                                        .addHeaderWriter(new CustomCacheControlHeadersWriter())
                                        // let's disable frame options by default ...
                                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                                        // ... and then add our writer that will set DENY by default and let any other value if the devs choose one
                                        .addHeaderWriter(new XFrameOptionsHeaderWriterDefault()))
            // Allow some static endpoints
            .authorizeHttpRequests(authz -> authz.requestMatchers(noSecurityRoutes.toArray(new String[0])).permitAll());
        // Add custom authorizeRequests configurations
        if (customAuthorizeRequestsConfigurers != null) {
            for (ICustomWebSecurityAuthorizeRequestsConfiguration customConfigurer : customAuthorizeRequestsConfigurers) {
                customConfigurer.configure(http);
            }
        }

        // Disable CSRF
        http.csrf(AbstractHttpConfigurer::disable)
            // Allow all request only if users are authenticated
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            // Add Basic filter to manage authentication endpoint
            .addFilterBefore(new BasicAuthenticationFilter(clientUser, clientSecret),
                             UsernamePasswordAuthenticationFilter.class)
            // Add public filter permitting to inject a PUBLIC JWT token to routes without security or without token
            .addFilterAfter(new PublicAuthenticationFilter(jwtService, noSecurityRoutes),
                            BasicAuthenticationFilter.class)
            // Add JWT Authentication filter parsing JWT token
            .addFilterAfter(new JWTAuthenticationFilter(authenticationManager, runtimeTenantResolver, noSecurityRoutes),
                            PublicAuthenticationFilter.class) //
            // For adding transfer infos to MDC (Mapped Diagnostic Context) to be used by slf4j
            .addFilterBefore(new MDCInsertingServletFilter(), JWTAuthenticationFilter.class)
            // For logging request infos
            .addFilterAfter(new RequestLogFilter(), JWTAuthenticationFilter.class)
            // Add Ip filter after Authentication filter (addresses can be filtered by role)
            .addFilterAfter(new IPFilter(authorizationService, noSecurityRoutes), JWTAuthenticationFilter.class)
            // Add CORS filter
            .addFilterAfter(new CorsFilter(authorizedIps()), IPFilter.class);
        // Add custom filters
        if (customFilterConfigurers != null) {
            for (final ICustomWebSecurityFilterConfiguration customFilterConfigurer : customFilterConfigurers) {
                customFilterConfigurer.configure(http, noSecurityRoutes);
            }
        }
        return http.build();
    }

    private List<String> authorizedIps() {
        final List<String> authorizedIp = new ArrayList<>();
        if (corsRequestAuthorizedClientAddresses != null) {
            for (final String ip : corsRequestAuthorizedClientAddresses.split(",")) {
                if (!ip.isEmpty()) {
                    authorizedIp.add(ip);
                }
            }
        }
        return authorizedIp;
    }

    @Bean
    public SecurityResourcesController securityController() {
        return new SecurityResourcesController(authorizationService);
    }

    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider(jwtService);
    }

    @Bean
    public JWTAuthenticationManager authenticationManager() {
        return new JWTAuthenticationManager(jwtService);
    }
}
