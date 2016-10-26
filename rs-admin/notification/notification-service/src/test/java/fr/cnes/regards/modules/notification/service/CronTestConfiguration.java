/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class for {@link CronTest}.
 *
 * @author xbrochar
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:test.properties")
@ComponentScan
public class CronTestConfiguration {

}
