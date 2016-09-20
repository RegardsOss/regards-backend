/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Configuration class to define the default PostgresSQL Data base
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class DataSourceConfig {

    @Autowired
    private MultitenancyProperties multitenancyProperties;

    @Bean(name = { "dataSource" })
    @ConfigurationProperties(prefix = "spring.multitenancy.datasource")
    public DataSource dataSource() {
        DataSourceBuilder factory = DataSourceBuilder
                .create(this.multitenancyProperties.getDatasource().getClassLoader())
                .driverClassName(this.multitenancyProperties.getDatasource().getDriverClassName())
                .username(this.multitenancyProperties.getDatasource().getUsername())
                .password(this.multitenancyProperties.getDatasource().getPassword())
                .url(this.multitenancyProperties.getDatasource().getUrl());
        return factory.build();
    }

}
