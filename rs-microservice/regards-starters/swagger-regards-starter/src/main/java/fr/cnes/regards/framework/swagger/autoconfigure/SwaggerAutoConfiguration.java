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
package fr.cnes.regards.framework.swagger.autoconfigure;

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.swagger.autoconfigure.override.ModelResolverCustom;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.*;
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

import java.util.Set;

/**
 * Auto configuration for swagger
 *
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

    private static final String AUTHENTICATION_KEY = "REGARDS_OAUTH2";

    @Value("${spring.application.name}")
    private String springAppName;

    @Value("${prefix.path}")
    private String prefixPath;

    @Value("${regards.instance.tenant.name:instance}")
    private String instanceTenant;

    @Autowired
    private SwaggerProperties properties;

    @Autowired
    private ITenantResolver tenantResolver;

    @Bean
    public FilterRegistrationBean<OpenApiFilter> openApiFilterRegistration() {
        FilterRegistrationBean<OpenApiFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OpenApiFilter());
        registrationBean.addUrlPatterns("/v3/api-docs");
        return registrationBean;
    }

    @Value("${regards.swagger.host:http\\://127.0.0.1}")
    private String regardsSwaggerHost;

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        overrideSwaggerSchemaConverter();
        return new OpenAPI().components(new Components().addSecuritySchemes(AUTHENTICATION_KEY,
                                                                            new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                                                                                .flows(new OAuthFlows().password(
                                                                                                    new OAuthFlow().tokenUrl(
                                                                                                                       String.format(
                                                                                                                           "%s%s/rs-authentication/oauth/token",
                                                                                                                           regardsSwaggerHost,
                                                                                                                           prefixPath))
                                                                                                                   .scopes(
                                                                                                                       getScopes())))))
                            .addSecurityItem(new SecurityRequirement().addList(AUTHENTICATION_KEY))
                            .info(new Info().title(properties.getApiTitle())
                                            .version(properties.getApiVersion())
                                            .description(properties.getApiDescription())
                                            .license(new License().name(properties.getApiLicense())));
    }

    /**
     * Hack to change swagger schema model converter in order to avoid use of Jackson annotations.
     * This hack is needed until REGARDS use jackson instead of Gson.<br/>
     * <br/>
     * This hack allows for example to ignore JsonUnwrapped jackson annotation in hateoas EntiytModel
     * domain when creating open api schema. With the default ModelResolver of swagger lin the
     * "content" section is not generated and content is unwrapped on the root object.
     *
     * @see <a href="https://stackoverflow.com/questions/72116316/openapi-scheme-generated-does-not-contains-content-of-entitymodel-spring-hateo">
     * swagger not compatible with gson
     * </a>
     */
    private void overrideSwaggerSchemaConverter() {
        ModelConverters.getInstance().addConverter(new ModelResolverCustom(Json.mapper()));
    }

    private Scopes getScopes() {
        if (properties.isInstance()) {
            return new Scopes().addString(instanceTenant, "");
        }

        Set<String> tenants = tenantResolver.getAllActiveTenants();
        Scopes scopes = new Scopes();
        tenants.forEach(tenant -> scopes.addString(tenant, ""));
        return scopes;
    }
}
