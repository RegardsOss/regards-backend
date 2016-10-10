/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

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
@ConditionalOnProperty("microservice.dao.enabled")
public class DataSourcesConfiguration {

    /**
     * Hibernate dialect for embedded HSQL Database
     */
    public static final String EMBEDDED_HSQLDB_HIBERNATE_DIALECT = "org.hibernate.dialect.HSQLDialect";

    /**
     * Hibernate driver class for embedded HSQL Database
     */
    public static final String EMBEDDED_HSQL_DRIVER_CLASS = "org.hsqldb.jdbcDriver";

    /**
     * Url prefix for embedded HSQL Database. Persistence into file.
     */
    public static final String EMBEDDED_HSQL_URL = "jdbc:hsqldb:file:";

    /**
     * Data source URL separator
     */
    public static final String EMBEDDED_URL_SEPARATOR = "/";

    /**
     * HSQL Embedded Data source base name. Property shutdown allow to close the embedded database when the last
     * connection is close.
     */
    public static final String EMBEDDED_URL_BASE_NAME = "applicationdb;shutdown=true;";

    /**
     * Microservice globale configuration
     */
    @Autowired
    private MicroserviceConfiguration configuration;

    /**
     *
     * List of data sources for each configured project.
     *
     * @return Map<Tenant, DataSource>
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = { "dataSources" })
    public Map<String, DataSource> getDataSources() {

        final Map<String, DataSource> datasources = new HashMap<>();

        for (final ProjectConfiguration project : configuration.getProjects()) {
            if (configuration.getDao().getEmbedded()) {
                final DriverManagerDataSource dataSource = new DriverManagerDataSource();
                dataSource.setDriverClassName(EMBEDDED_HSQL_DRIVER_CLASS);
                dataSource.setUrl(EMBEDDED_HSQL_URL + configuration.getDao().getEmbeddedPath()
                        + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR + project.getName()
                        + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR
                        + DataSourcesConfiguration.EMBEDDED_URL_BASE_NAME);
                datasources.put(project.getName(), dataSource);
            } else {
                final DataSourceBuilder factory = DataSourceBuilder.create(project.getDatasource().getClassLoader())
                        .driverClassName(configuration.getDao().getDriverClassName())
                        .username(project.getDatasource().getUsername()).password(project.getDatasource().getPassword())
                        .url(project.getDatasource().getUrl());
                datasources.put(project.getName(), factory.build());
            }
        }

        return datasources;
    }

    /**
     *
     * Default data source for persistence unit projects.
     *
     * @return datasource
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public DataSource projectsDataSource() {

        DataSource datasource = null;
        final ProjectConfiguration project = configuration.getProjects().get(0);

        if (configuration.getDao().getEmbedded()) {
            final DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(EMBEDDED_HSQL_DRIVER_CLASS);
            dataSource.setUrl(EMBEDDED_HSQL_URL + configuration.getDao().getEmbeddedPath()
                    + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR + project.getName()
                    + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR
                    + DataSourcesConfiguration.EMBEDDED_URL_BASE_NAME);

            datasource = dataSource;
        } else {
            final DataSourceBuilder factory = DataSourceBuilder.create(project.getDatasource().getClassLoader())
                    .driverClassName(configuration.getDao().getDriverClassName())
                    .username(project.getDatasource().getUsername()).password(project.getDatasource().getPassword())
                    .url(project.getDatasource().getUrl());
            datasource = factory.build();
        }
        return datasource;
    }

    /**
     *
     * Default data source for persistence unit instance.
     *
     * @return datasource
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnProperty("microservice.dao.instance.enabled")
    public DataSource instanceDataSource() {

        DataSource datasource = null;
        if (configuration.getDao().getEmbedded()) {
            final DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(EMBEDDED_HSQL_DRIVER_CLASS);
            dataSource.setUrl(EMBEDDED_HSQL_URL + configuration.getDao().getEmbeddedPath()
                    + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR + "instance"
                    + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR
                    + DataSourcesConfiguration.EMBEDDED_URL_BASE_NAME);
            datasource = dataSource;
        } else {
            final DataSourceBuilder factory = DataSourceBuilder
                    .create(configuration.getDao().getInstance().getDatasource().getClassLoader())
                    .driverClassName(configuration.getDao().getDriverClassName())
                    .username(configuration.getDao().getInstance().getDatasource().getUsername())
                    .password(configuration.getDao().getInstance().getDatasource().getPassword())
                    .url(configuration.getDao().getInstance().getDatasource().getUrl());
            datasource = factory.build();
        }
        return datasource;
    }

}
