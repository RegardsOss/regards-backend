/**
 *
 */
package fr.cnes.regards.framework.modules.jobs.rest;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
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
// FIXME do it in the future JOB starter
@ComponentScan(basePackages = { "fr.cnes.regards.framework.modules.jobs" })
public class JobControllerTestConfiguration {
}