/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author svissier
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.security.utils", "fr.cnes.regards.modules.core",
        "fr.cnes.regards.modules.project" })
@PropertySource("classpath:application.properties")
public class AmqpTestsConfiguration {
}
