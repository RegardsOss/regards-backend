/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import java.util.HashMap;
import java.util.Map;

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

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.dao.hibernate.CurrentTenantIdentifierResolverImpl;
import fr.cnes.regards.microservices.core.dao.jpa.DataSourceConfig;

@Configuration
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CurrentTenantIdentifierResolverImpl.class) })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        WebClientAutoConfiguration.class, DataSourceConfig.class })
@EnableConfigurationProperties(MicroserviceConfiguration.class)
@PropertySource("classpath:dao.properties")
public class MultiTenancyDaoTestConfiguration {

    @Bean(name = { "dataSources" })
    public Map<String, DataSource> dataSources() {

        Map<String, DataSource> datasources = new HashMap<>();

        final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        datasources.put("test1", builder.setType(EmbeddedDatabaseType.HSQL).setName("dataSource").build());

        final EmbeddedDatabaseBuilder builder2 = new EmbeddedDatabaseBuilder();
        datasources.put("test2", builder2.setType(EmbeddedDatabaseType.HSQL).setName("dataSource2").build());

        return datasources;
    }

    @Bean
    public DataSource defaultDataSource() {
        final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder.setType(EmbeddedDatabaseType.HSQL).setName("dataSource").build();
    }

}
