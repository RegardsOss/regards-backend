/**
 *
 */
package fr.cnes.regards.framework.modules.jobs.service;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
@PropertySource("classpath:application-rabbit.properties")
public class JobServiceTestConfiguration {

}