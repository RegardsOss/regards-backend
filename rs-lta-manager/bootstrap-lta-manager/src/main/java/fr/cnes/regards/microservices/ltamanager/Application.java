package fr.cnes.regards.microservices.ltamanager;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start microservice rs-lta-manager
 *
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "lta-manager", version = "1.11.0-SNAPSHOT")
public class Application {

    /**
     * Microservice bootstrap method
     *
     * @param pArgs microservice bootstrap arguments
     */
    public static void main(final String[] pArgs) {
        SpringApplication.run(Application.class, pArgs); // NOSONAR
    }

}
