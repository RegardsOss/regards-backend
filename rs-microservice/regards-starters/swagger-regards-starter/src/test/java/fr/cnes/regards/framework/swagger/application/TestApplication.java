/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.swagger.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * Mock to test Swagger API rendering with GSON starter on the classpath
 * 
 * @author Marc Sordi
 *
 */
// CHECKSTYLE:OFF
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.framework.swagger.application" })
public class TestApplication {

    public static void main(final String[] pArgs) {
        SpringApplication.run(TestApplication.class, pArgs); // NOSONAR
    }
}
