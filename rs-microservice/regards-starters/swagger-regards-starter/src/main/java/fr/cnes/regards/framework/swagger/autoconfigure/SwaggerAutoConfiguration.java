/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.swagger.autoconfigure;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Auto configuration for swagger
 * @author msordi
 */
@Configuration
@ConditionalOnWebApplication
@Conditional(ServerProperties.class)
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(prefix = "regards.swagger", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(value = MultitenantAutoConfiguration.class)
public class SwaggerAutoConfiguration {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerAutoConfiguration.class);

    @Value("${spring.application.name}")
    private String springAppName;

    @Value("${zuul.prefix}")
    private String zuulPrefix;

    @Autowired
    private SwaggerProperties properties;

    @Autowired
    private ITenantResolver tenantResolver;

    @Bean
    public FilterRegistrationBean<OpenApiFilter> loggingFilter() {
        FilterRegistrationBean<OpenApiFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OpenApiFilter());
        registrationBean.addUrlPatterns("/v3/api-docs");
        return registrationBean;
    }

    @Value("${regards.swagger.host:127.0.0.1}")
    private String regardsSwaggerHost;

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("REGARDS", new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().password(new OAuthFlow().tokenUrl(String
                                .format("http://%s%s/rs-authentication/oauth/token", regardsSwaggerHost, zuulPrefix))
                                .scopes(getScopes())))))
                .info(new Info().title(properties.getApiTitle()).version(properties.getApiVersion())
                        .description(properties.getApiDescription())
                        .license(new License().name(properties.getApiLicense())));
    }

    private Scopes getScopes() {
        Set<String> tenants = tenantResolver.getAllActiveTenants();
        Scopes scopes = new Scopes();
        tenants.forEach(tenant -> scopes.addString(tenant, ""));
        return scopes;
    }
}
