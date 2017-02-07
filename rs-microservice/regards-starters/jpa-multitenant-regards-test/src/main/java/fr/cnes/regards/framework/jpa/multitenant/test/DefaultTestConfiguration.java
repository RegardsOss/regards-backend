/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Default JPA multitenant test configuration
 *
 * @author Marc Sordi
 *
 */
@Configuration
@EnableAutoConfiguration(exclude = JacksonAutoConfiguration.class)
@PropertySource("classpath:dao.properties")
public class DefaultTestConfiguration {

}
