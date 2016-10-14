/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.jpa.configuration.MicroserviceConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.hibernate.CurrentTenantIdentifierResolverImpl;

/**
 *
 * Class MultiTenancyDaoTestConfiguration
 *
 * Configuration class for DAO unit tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant" }, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CurrentTenantIdentifierResolverImpl.class) })
@EnableConfigurationProperties(MicroserviceConfiguration.class)
@PropertySource("classpath:dao.properties")
public class MultiTenancyDaoTestConfiguration {

}
