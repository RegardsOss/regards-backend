/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.multitransactional;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Marc Sordi
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant.autoconfigure.multitransactional" })
@EnableAutoConfiguration
@PropertySource({ "classpath:multi-transaction.properties" })
public class MultiTransactionTestConfiguration {

}
