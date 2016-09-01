package fr.cnes.regards.microservices.${artifactId};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import springfox.documentation.builders.ApiInfoBuilder;

/**
 * Main class to start Spring boot application for microservice ${artifactId}
 */
@SpringBootApplication(scanBasePackages={"fr.cnes.regards.modules", "fr.cnes.regards.microservices.core"})
public class Application {
	
	@Bean
	public ApiInfoBuilder apiInfoBuilder() {
        return new ApiInfoBuilder()
                .title("${artifactId} API")
                .description("API for ${artifactId} REGARDS Microservice")
                .license("Apache License Version 2.0")
                .version("${version}");
    }
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
