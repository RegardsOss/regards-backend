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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import springfox.documentation.PathProvider;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.ResourceOwnerPasswordCredentialsGrant;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.AbstractPathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Auto configuration for swagger
 * @author msordi
 */
@Configuration
@ConditionalOnWebApplication
@Conditional(ServerProperties.class)
@ConditionalOnProperty(prefix = "regards.swagger", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SwaggerProperties.class)
@EnableSwagger2
public class SwaggerAutoConfiguration {

    private static final String TENANT_DESC = "This scope corresponds to %s project";

    @Value("${spring.application.name}")
    private String springAppName;

    @Autowired
    private SwaggerProperties properties;

    @Autowired
    private ITenantResolver tenantResolver;

    @Value("${regards.swagger.host:127.0.0.1}")
    private String regardsSwaggerHost;

    private ApiInfoBuilder apiInfoBuilder() {
        return new ApiInfoBuilder().title(properties.getApiTitle()).description(properties.getApiDescription())
                .license(properties.getApiLicense()).version(properties.getApiVersion());
    }

    @Bean
    public Docket appApi() {

        final ApiInfo infos = apiInfoBuilder().build();

        return new Docket(DocumentationType.SWAGGER_2).protocols(Sets.newHashSet("http")).host(regardsSwaggerHost).apiInfo(infos)
                .pathProvider(regardsPathProvider()).select().apis(RequestHandlerSelectors.basePackage("fr.cnes.regards"))
                .paths(PathSelectors.any()).build().securitySchemes(Collections.singletonList(securityScheme()))
                .securityContexts(Collections.singletonList(securityContext()));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(Collections.singletonList(new SecurityReference("REGARDS OAuth", scopes())))
                .forPaths(PathSelectors.any()).build();
    }

    private SecurityScheme securityScheme() {
        GrantType grantType = new ResourceOwnerPasswordCredentialsGrant(
                "http://127.0.0.1/api/v1/rs-authentication/oauth/token");

        return new OAuthBuilder().name("REGARDS OAuth").grantTypes(Arrays.asList(grantType))
                .scopes(Arrays.asList(scopes())).build();
    }

    private AuthorizationScope[] scopes() {
//        Set<String> activeTenants = tenantResolver.getAllActiveTenants();
//        return activeTenants.stream().map(tenant -> new AuthorizationScope(tenant, String.format(TENANT_DESC, tenant)))
//                .toArray(AuthorizationScope[]::new);
        AuthorizationScope[] defaultScopes = { new AuthorizationScope("instance", "desc instance"),
                new AuthorizationScope("project1", "desc project1") };
        return defaultScopes;
    }

    @Bean
    @Primary
    public PathProvider regardsPathProvider() {
        return new RegardsPathProvider(springAppName);
    }

    @Bean
    public PluginRegistry<LinkDiscoverer, MediaType> discoverers(
            OrderAwarePluginRegistry<LinkDiscoverer, MediaType> relProviderPluginRegistry) {
        return relProviderPluginRegistry;
    }

    private class RegardsPathProvider extends AbstractPathProvider {

        private final String springAppName;

        public RegardsPathProvider(String springAppName) {
            this.springAppName = springAppName;
        }

        @Override
        protected String applicationPath() {
            return "/api/v1/" + springAppName;
        }

        @Override
        protected String getDocumentationPath() {
            return "/";
        }
    }
}
