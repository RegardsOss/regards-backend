/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * Class MultiTenancyDaoTestConfiguration
 *
 * Configuration class for DAO unit tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant" })
@EnableAutoConfiguration
@PropertySource("classpath:dao.properties")
public class MultiTenancyDaoTestConfiguration {

}
