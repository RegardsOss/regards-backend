package fr.cnes.regards.microservices.backend;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import springfox.documentation.builders.ApiInfoBuilder;

/**
 * Main class to start Spring boot application for microservice backend-mock
 */
@SpringBootApplication(scanBasePackages={"fr.cnes.regards.microservices.backend","fr.cnes.regards.microservices.core"})
public class Application {
	
	@Bean
	public ApiInfoBuilder apiInfoBuilder() {
        return new ApiInfoBuilder()
                .title("backend-mock API")
                .description("API for backend-mock REGARDS Microservice")
                .license("Apache License Version 2.0")
                .version("1.0-SNAPSHOT");
    }
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
