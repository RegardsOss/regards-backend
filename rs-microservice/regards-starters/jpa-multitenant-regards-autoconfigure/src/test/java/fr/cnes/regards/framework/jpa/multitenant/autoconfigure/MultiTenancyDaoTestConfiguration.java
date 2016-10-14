/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;

/**
 *
 * Class MultiTenancyDaoTestConfiguration
 *
 * Configuration class for DAO unit tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ComponentScan(
        basePackages = { "fr.cnes.regards.framework.jpa.multitenant", "fr.cnes.regards.framework.security.utils" })
@EnableConfigurationProperties(MultitenantDaoProperties.class)
@PropertySource("classpath:dao.properties")
public class MultiTenancyDaoTestConfiguration {

}
