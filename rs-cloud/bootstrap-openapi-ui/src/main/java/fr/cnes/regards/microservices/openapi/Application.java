/*
 *
 *  * Copyright 2019-2020 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package fr.cnes.regards.microservices.openapi;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;

@SpringBootApplication
@ComponentScan(basePackages = { "org.springdoc.demo.app2" })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @Bean
    public GroupedOpenApi userOpenApi() {
        String[] paths = { "/user/**" };
        String[] packagedToMatch = { "org.springdoc.demo.app2" };
        return GroupedOpenApi.builder().setGroup("users").pathsToMatch(paths).packagesToScan(packagedToMatch).build();
    }

    @Bean
    public GroupedOpenApi storeOpenApi() {
        String[] paths = { "/store/**" };
        return GroupedOpenApi.builder().setGroup("stores").pathsToMatch(paths).build();
    }

    // FIXME configure OAuth parameters dynamically
    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        return new OpenAPI().externalDocs(new ExternalDocumentation().url("rs-admin/v3/api-docs?scope=public"))
                .components(new Components()
                        .addSecuritySchemes("REGARDS", new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().password(new OAuthFlow()
                                        //                                .authorizationUrl("http://172.26.47.176:9030/api/v1/rs-authentication/oauth/token")
                                        // .tokenUrl("http://172.26.47.176:9030/api/v1/rs-authentication/oauth/token")
                                        .tokenUrl("http://localhost:9030/api/v1/rs-authentication/oauth/token")
                                        .scopes(new Scopes().addString("project1", "target project"))))))
                .info(new Info().title("REGARDS API").version(appVersion).description("TODO Description")
                        // .termsOfService("http://swagger.io/terms/")
                        .license(new License().name("GPLV3").url("https://github.com/regardsOss")));
    }

}
