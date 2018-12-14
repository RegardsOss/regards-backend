package fr.cnes.regards.cloud.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Class RegistryApplication
 *
 * Eureka Registry server
 * @author Sébastien Binda
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableEurekaServer
public class Application { // NOSONAR

    /**
     * Starter method
     * @param pArgs arguments
     */
    public static void main(final String[] pArgs) {
        SpringApplication.run(Application.class, pArgs); // NOSONAR
    }
}
