/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Default Amqp Regards Appender test configuration
 *
 * @author Christophe Mertz
 *
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:amqp-rabbit.properties")
public class DefaultTestConfiguration {

}
