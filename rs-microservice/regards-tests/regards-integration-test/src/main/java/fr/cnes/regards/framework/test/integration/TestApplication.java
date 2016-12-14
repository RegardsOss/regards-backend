/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mock to test API endpoints at module level
 *
 * @author msordi
 *
 */
// CHECKSTYLE:OFF
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.microservices" })
public class TestApplication {

    public static void main(final String[] pArgs) {
        SpringApplication.run(TestApplication.class, pArgs); // NOSONAR
    }
}
