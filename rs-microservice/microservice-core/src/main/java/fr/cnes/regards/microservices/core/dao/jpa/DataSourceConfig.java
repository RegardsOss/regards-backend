/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.configuration.common.ProjectConfiguration;

/**
 *
 * Configuration class to define the default PostgresSQL Data base
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableConfigurationProperties(MicroserviceConfiguration.class)
public class DataSourceConfig {

    @Autowired
    private MicroserviceConfiguration configuration_;

    @Bean(name = { "dataSources" })
    public Map<String, DataSource> dataSources() {

        Map<String, DataSource> datasources = new HashMap<>();

        for (ProjectConfiguration project : configuration_.getProjects()) {
            if (project.getDatasource() != null) {
                DataSourceBuilder factory = DataSourceBuilder.create(project.getDatasource().getClassLoader())
                        .driverClassName(project.getDatasource().getDriverClassName())
                        .username(project.getDatasource().getUsername()).password(project.getDatasource().getPassword())
                        .url(project.getDatasource().getUrl());
                datasources.put(project.getName(), factory.build());
            }
            else {
                final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
                datasources.put(project.getName(),
                                builder.setType(EmbeddedDatabaseType.HSQL).setName(project.getName()).build());
            }
        }

        return datasources;
    }

    @Bean
    public DataSource defaultDataSource() {
        DataSource datasource = null;
        ProjectConfiguration project = configuration_.getProjects().get(0);
        if (project.getDatasource() != null) {
            DataSourceBuilder factory = DataSourceBuilder.create(project.getDatasource().getClassLoader())
                    .driverClassName(project.getDatasource().getDriverClassName())
                    .username(project.getDatasource().getUsername()).password(project.getDatasource().getPassword())
                    .url(project.getDatasource().getUrl());
            datasource = factory.build();
        }
        else {
            final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.HSQL).setName(project.getName()).build();
        }
        return datasource;
    }

}
