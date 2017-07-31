/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;

/**
 * Start microservice order
 * @author oroussel
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "order", version = "1.0-SNAPSHOT")
public class Application {

    /**
     * Microservice bootstrap method
     * @param args microservice bootstrap arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args); // NOSONAR
    }

}
