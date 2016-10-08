/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.swagger;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.ImplicitGrant;
import springfox.documentation.service.LoginEndpoint;
import springfox.documentation.service.OAuth;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 *
 * Class SwaggerConfiguration
 *
 * Configuration class for swagger
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableSwagger2 // Enable swagger 2.0 spec
public class SwaggerConfiguration {

    /**
     * Security schema for oauth2 swagger configuration
     */
    public static final String SECURITY_SCHEMA_OAUTH2 = "oauth2schema";

    /**
     * global scope
     */
    public static final String AUTH_SCOPE_GLOBAL = "global";

    /**
     * global scope description
     */
    public static final String AUTH_SCOPE_GLOBAL_DESC = "accessEverything";

    /**
     * Swagger api name
     */
    @Value("${swagger.api.name:unknwon}")
    private String swaggerApiName;

    /**
     * Microservice port
     */
    @Value("${server.port}")
    private String serverPort;

    /**
     * Microservice adress
     */
    @Value("${server.adress}")
    private String serverAdress;

    /**
     * Swagger api informations
     */
    @Autowired
    private ApiInfoBuilder apiInfoBuilder;

    /**
     *
     * Create spring bean with swagger api informations.
     *
     * @return Docket
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public Docket appApi() {

        final List<OAuth> schemes = new ArrayList<>();
        schemes.add(securitySchema());

        final List<SecurityContext> ctxs = new ArrayList<>();
        ctxs.add(securityContext());

        final ApiInfo infos = apiInfoBuilder.termsOfServiceUrl(String.format("http://%s:%s", serverAdress,
                                                                              serverPort)).build();

        return new Docket(DocumentationType.SWAGGER_2).groupName(swaggerApiName).apiInfo(infos).select()
                .paths(apiPaths()).build().securitySchemes(schemes).securityContexts(ctxs);
    }

    /**
     *
     * Get microservice api path
     *
     * @return List of allowed api paths
     * @since 1.0-SNAPSHOT
     */
    private Predicate<String> apiPaths() {
        return PathSelectors.regex("/(api|config|eureka).*");
    }

    /**
     *
     * Generate security Oauth schema
     *
     * @return OAuth
     * @since 1.0-SNAPSHOT
     */
    private OAuth securitySchema() {
        final AuthorizationScope authorizationScope = new AuthorizationScope(AUTH_SCOPE_GLOBAL, AUTH_SCOPE_GLOBAL_DESC);
        final LoginEndpoint loginEndpoint = new LoginEndpoint("http://" + serverAdress + ":" + serverPort
                + "/oauth/token");
        final GrantType grantType = new ImplicitGrant(loginEndpoint, "access_token");
        final List<AuthorizationScope> authList = new ArrayList<>();
        authList.add(authorizationScope);
        final List<GrantType> grants = new ArrayList<>();
        grants.add(grantType);

        return new OAuth(SECURITY_SCHEMA_OAUTH2, authList, grants);
    }

    /**
     *
     * Get Security context
     *
     * @return SecurityContext
     * @since 1.0-SNAPSHOT
     */
    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(apiPaths()).build();
    }

    /**
     *
     * Get the default authentication
     *
     * @return SecurityReferences
     * @since 1.0-SNAPSHOT
     */
    private List<SecurityReference> defaultAuth() {
        final AuthorizationScope authorizationScope = new AuthorizationScope(AUTH_SCOPE_GLOBAL, AUTH_SCOPE_GLOBAL_DESC);
        final AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        final List<SecurityReference> refs = new ArrayList<>();
        refs.add(new SecurityReference(SECURITY_SCHEMA_OAUTH2, authorizationScopes));
        return refs;
    }

}
