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

import java.util.Arrays;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

@SpringBootApplication
@ComponentScan(basePackages = { "fr.cnes.regards.microservices.openapi.web" })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

        @Bean
        public GroupedOpenApi userOpenApi() {
            String[] paths = { "/**" };
    //        String[] packagedToMatch = { "fr.cnes.regards" };
    //        return GroupedOpenApi.builder().setGroup("adminInstance").pathsToMatch(paths).packagesToScan(packagedToMatch).build();
            return GroupedOpenApi.builder().setGroup("adminInstance").pathsToMatch(paths).build();

        }

        // FIXME configure OAuth parameters dynamically
        @Bean
        public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
            Server adminInstanceServer = new Server();
            adminInstanceServer.setUrl("http://127.0.0.1/api/v1/rs-admin-instance/v2/api-docs");
            adminInstanceServer.setDescription("Admin Instance OpenAPI");
            return new OpenAPI().servers(Arrays.asList(adminInstanceServer));
    //        .externalDocs(new ExternalDocumentation().url("http://127.0.0.1/api/v1/rs-admin-instance/v2/api-docs?scope=public"))
    //                .components(new Components()
    ////                        .addSecuritySchemes("REGARDS", new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
    ////                                .flows(new OAuthFlows().password(new OAuthFlow()
    ////                                        //                                .authorizationUrl("http://172.26.47.176:9030/api/v1/rs-authentication/oauth/token")
    ////                                        // .tokenUrl("http://172.26.47.176:9030/api/v1/rs-authentication/oauth/token")
    ////                                        .tokenUrl("http://localhost:9030/api/v1/rs-authentication/oauth/token")
    ////                                        .scopes(new Scopes().addString("project1", "target project")))))
    //                                     )
    //                .info(new Info().title("REGARDS API").version(appVersion).description("TODO Description")
    //                        // .termsOfService("http://swagger.io/terms/")
    //                        .license(new License().name("GPLV3").url("https://github.com/regardsOss")));
        }

}
