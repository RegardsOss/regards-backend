/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;

/**
 * 
 * Start microservice catalog
 * @author TODO
 *
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "catalog", version = "1.0-SNAPSHOT")
public class Application {

    /**
     * Microservice bootstrap method
     *
     * @param pArgs
     *            microservice bootstrap arguments
     */
    public static void main(final String[] pArgs) {
        SpringApplication.run(Application.class, pArgs); // NOSONAR
    }

}
