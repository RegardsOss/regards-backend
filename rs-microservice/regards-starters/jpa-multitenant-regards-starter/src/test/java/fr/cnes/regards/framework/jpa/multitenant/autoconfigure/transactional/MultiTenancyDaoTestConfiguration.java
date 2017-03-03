/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional;

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
@ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional" })
@EnableAutoConfiguration
@PropertySource({ "classpath:dao.properties", "classpath:default-amqp.properties" })
public class MultiTenancyDaoTestConfiguration {

}
