package fr.cnes.regards.microservices.administration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import fr.cnes.regards.microservices.core.information.MicroserviceInfo;
import springfox.documentation.builders.ApiInfoBuilder;

/**
 * Main class to start Spring boot application for microservice administration
 */
@SpringBootApplication(scanBasePackages={"fr.cnes.regards.modules", "fr.cnes.regards.microservices.core"})
@MicroserviceInfo(name="administration", version="1.0-SNAPSHOT")
public class Application {
	
	@Bean
	public ApiInfoBuilder apiInfoBuilder() {
        return new ApiInfoBuilder()
                .title("administration API")
                .description("API for administration REGARDS Microservice")
                .license("Apache License Version 2.0")
                .version("1.0-SNAPSHOT");
    }
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
