/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.test.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;

import fr.cnes.regards.framework.starter.swagger.SwaggerConfiguration;

import org.springframework.context.annotation.FilterType;

/**
 * Mock to test API endpoints at module level
 *
 * @author msordi
 *
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.microservices.core",
        "fr.cnes.regards.security.utils" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                SwaggerConfiguration.class }))
public class TestApplication {

    public static void main(final String[] pArgs) {
        SpringApplication.run(TestApplication.class, pArgs); // NOSONAR
    }
}
