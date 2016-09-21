/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.microservices.access;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import fr.cnes.regards.microservices.core.annotation.MicroserviceInfo;
import springfox.documentation.builders.ApiInfoBuilder;

/**
 * Main class to start Spring boot application for microservice access
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.microservices.core" })
@MicroserviceInfo(name = "access", version = "1.0-SNAPSHOT")
public class Application {

    /**
     * API for access REGARDS Microservice
     * @return ApiInfoBuilder
     */
    @Bean
    public ApiInfoBuilder apiInfoBuilder() {
        return new ApiInfoBuilder().title("access API").description("API for access REGARDS Microservice")
                .license("Apache License Version 2.0").version("1.0-SNAPSHOT");
    }

    /**
     * API for access REGARDS Microservice main function 
     * @param pArgs args
     */
    public static void main(String[] pArgs) {
        SpringApplication.run(Application.class, pArgs);
    }

}
