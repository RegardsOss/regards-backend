/**
 *
 */
package fr.cnes.regards.modules.jobs.service;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class JobDaoTestConfiguration {

}