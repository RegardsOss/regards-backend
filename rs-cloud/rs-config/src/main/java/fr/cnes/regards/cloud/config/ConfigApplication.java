/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * Class ConfigApplication
 *
 * Boostrap for Spring Configuration Server
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigApplication {

    /**
     * .
     *
     * Starter method
     *
     * @param pArgs
     *            params
     * @since 1.0-SNAPSHOT
     */
    public static void main(String[] pArgs) {
        final ConfigurableApplicationContext context = SpringApplication.run(ConfigApplication.class, pArgs);
        context.close();
    }
}