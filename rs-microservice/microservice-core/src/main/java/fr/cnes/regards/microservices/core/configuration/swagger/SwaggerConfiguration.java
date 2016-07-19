package fr.cnes.regards.microservices.core.configuration.swagger;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;

import static springfox.documentation.builders.PathSelectors.regex;

import springfox.documentation.builders.ApiInfoBuilder;
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

@Configuration
@EnableSwagger2 //Enable swagger 2.0 spec
public class SwaggerConfiguration {
	
	public static final String securitySchemaOAuth2 = "oauth2schema";
    public static final String authorizationScopeGlobal = "global";
    public static final String authorizationScopeGlobalDesc ="accessEverything";
    
    @Value("${swagger.api.name}")
    private String swaggerApiName_="default";
    
    @Value("${server.port}")
    private String serverPort_;
    
    @Value("${server.adress}")
    private String serverAdress_;
    
    @Autowired
    private ApiInfoBuilder apiInfoBuilder_;
	
	@Bean
    public Docket AppApi() {

    	List<OAuth> schemes = new ArrayList<>();
    	schemes.add(securitySchema());
    	
    	List<SecurityContext> ctxs = new ArrayList<>();
    	ctxs.add(securityContext());
    	
    	ApiInfo infos = apiInfoBuilder_
    			.termsOfServiceUrl("http://"+serverAdress_+":"+serverPort_)
    			.build();
     	
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(swaggerApiName_)
                .apiInfo(infos)
                .select()
                .paths(apiPaths())
                .build()
                .securitySchemes(schemes)
        		.securityContexts(ctxs);
    }
    
    
    
    private Predicate<String> apiPaths() {
        return regex("/(api|config|eureka).*");
    }
    
    private OAuth securitySchema() {
        AuthorizationScope authorizationScope = new AuthorizationScope(authorizationScopeGlobal, authorizationScopeGlobal);
        LoginEndpoint loginEndpoint = new LoginEndpoint("http://"+serverAdress_+":"+serverPort_+"/oauth/token");
        GrantType grantType = new ImplicitGrant(loginEndpoint, "access_token");
        List<AuthorizationScope> AuthList = new ArrayList<>();
        AuthList.add(authorizationScope);
        List<GrantType> grants = new ArrayList<>();
        grants.add(grantType);
        		
        return new OAuth(securitySchemaOAuth2, AuthList, grants);
    }
    
    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(apiPaths())
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
                = new AuthorizationScope(authorizationScopeGlobal, authorizationScopeGlobalDesc);
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> refs = new ArrayList<>();
        refs.add(new SecurityReference(securitySchemaOAuth2, authorizationScopes));
        return refs;
    }

}
