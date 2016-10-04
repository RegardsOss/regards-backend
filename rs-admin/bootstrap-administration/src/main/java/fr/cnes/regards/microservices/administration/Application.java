/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import fr.cnes.regards.microservices.core.annotation.MicroserviceInfo;
import springfox.documentation.builders.ApiInfoBuilder;

/**
 *
 * Start microservice ${artifactId}
 *
 * @author TODO
 *
 */
<<<<<<< HEAD
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards" })
=======
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.microservices.core",
        "fr.cnes.regards.security.utils" })
>>>>>>> branch 'master' of https://sbinda@thor.si.c-s.fr/git/rs-admin
@MicroserviceInfo(name = "administration", version = "1.0-SNAPSHOT")
@ImportResource("classpath*:defaultRoles.xml")
public class Application {

    @Bean
    public ApiInfoBuilder apiInfoBuilder() {
        return new ApiInfoBuilder().title("administration API")
                .description("API for administration REGARDS Microservice").license("Apache License Version 2.0")
                .version("1.0-SNAPSHOT");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
