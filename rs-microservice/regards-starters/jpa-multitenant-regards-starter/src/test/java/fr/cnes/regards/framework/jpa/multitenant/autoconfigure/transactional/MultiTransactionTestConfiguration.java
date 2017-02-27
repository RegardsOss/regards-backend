/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Marc Sordi
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional" })
@EnableAutoConfiguration
@PropertySource({ "classpath:multi-transaction.properties" })
public class MultiTransactionTestConfiguration {

}
