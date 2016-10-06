package fr.cnes.regards.cloud.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * Class RegistryApplication
 *
 * Eureka Registry server
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableEurekaServer
public class Application {

    /**
     *
     * Starter method
     *
     * @param pArgs
     *            arguments
     * @since 1.0-SNAPSHOT
     */
    public static void main(String[] pArgs) {
        final ConfigurableApplicationContext context = SpringApplication.run(Application.class, pArgs);
        context.close();
    }
}
