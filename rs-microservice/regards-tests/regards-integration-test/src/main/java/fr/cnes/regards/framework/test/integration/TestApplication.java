/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import fr.cnes.regards.framework.swagger.autoconfigure.SwaggerAutoConfiguration;

/**
 * Mock to test API endpoints at module level
 *
 * @author msordi
 *
 */
// CHECKSTYLE:OFF
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules" }, exclude = { SwaggerAutoConfiguration.class })
public class TestApplication {

    public static void main(final String[] pArgs) {
        SpringApplication.run(TestApplication.class, pArgs); // NOSONAR
    }
}
