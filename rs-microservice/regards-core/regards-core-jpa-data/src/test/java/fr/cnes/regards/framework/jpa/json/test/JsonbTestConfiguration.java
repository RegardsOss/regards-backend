/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.json" })
@PropertySource("classpath:tests.properties")
public class JsonbTestConfiguration {

}
