/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.dao.service.DaoUserTest;
import fr.cnes.regards.microservices.core.dao.util.CurrentTenantIdentifierResolverMock;

@ComponentScan(basePackages = { "fr.cnes.regards.microservices.core.dao",
        "fr.cnes.regards.microservices.core.security" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                        CurrentTenantIdentifierResolverMock.class, DaoUserTest.class }) })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@EnableConfigurationProperties(MicroserviceConfiguration.class)
@PropertySource("classpath:dao.properties")
@PropertySource("classpath:jwt.properties")
public class MultiTenancyDaoITConfiguration {

}
