/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author svissier
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.framework.security.utils", "fr.cnes.regards.framework.amqp",
        "fr.cnes.regards.modules.project" })
@PropertySource("classpath:application.properties")
public class AmqpTestsConfiguration {
}
