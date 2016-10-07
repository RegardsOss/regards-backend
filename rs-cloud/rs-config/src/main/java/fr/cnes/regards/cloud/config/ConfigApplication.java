/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

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
    public static void main(final String[] pArgs) {
        SpringApplication.run(ConfigApplication.class, pArgs);
    }
}