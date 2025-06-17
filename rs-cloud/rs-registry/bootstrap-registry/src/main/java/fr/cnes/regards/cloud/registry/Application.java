package fr.cnes.regards.cloud.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Class RegistryApplication
 * <p>
 * Eureka Registry server
 *
 * @author Sébastien Binda
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableEurekaServer
public class Application { // NOSONAR

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @SuppressWarnings("java:S2221")  // main entrypoint: catch all to ensure clean exit
    public static void main(final String[] args) {
        try {
            SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            LOGGER.error("Going to exit", e);
            System.exit(1);
        }
    }
}
