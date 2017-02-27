/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event.basic;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;

/**
 * @author svissier
 *
 */
@Configuration
@ComponentScan(basePackageClasses = { MultitenantAutoConfiguration.class, AmqpAutoConfiguration.class })
@PropertySource({ "classpath:application.properties", "classpath:application-rabbit.properties" })
public class BasicTestConfiguration {
}
