/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import fr.cnes.regards.microservices.core.dao.hibernate.CurrentTenantIdentifierResolverImpl;
import fr.cnes.regards.microservices.core.dao.jpa.DataSourceConfig;
import fr.cnes.regards.microservices.core.dao.jpa.MultitenancyProperties;

@Configuration
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CurrentTenantIdentifierResolverImpl.class) })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        WebClientAutoConfiguration.class, DataSourceConfig.class })
@EnableConfigurationProperties(MultitenancyProperties.class)
@PropertySource("classpath:dao.properties")
public class MultiTenancyDaoTestConfiguration {

    @Bean(name = { "dataSource" })
    public DataSource getDataSource() {
        final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder.setType(EmbeddedDatabaseType.HSQL).setName("dataSource").build();
    }

    @Bean(name = { "dataSource2" })
    public DataSource getDataSource2() {
        final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder.setType(EmbeddedDatabaseType.HSQL).setName("dataSource2").build();
    }

}
