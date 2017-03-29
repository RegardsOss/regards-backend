/**
 *
 */
package fr.cnes.regards.framework.modules.jobs.rest;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * @author Christophe Mertz
 *
 */

@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
@PropertySource("classpath:application-rabbit.properties")
public class JobControllerTestConfiguration {
}