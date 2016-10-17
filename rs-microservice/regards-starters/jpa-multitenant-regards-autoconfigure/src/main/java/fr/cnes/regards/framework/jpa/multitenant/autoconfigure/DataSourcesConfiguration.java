/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConfiguration;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Configuration class to define the default PostgresSQL Data base
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableConfigurationProperties(MultitenantDaoProperties.class)
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class DataSourcesConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataSourcesConfiguration.class);

    /**
     * Microservice globale configuration
     */
    @Autowired
    private MultitenantDaoProperties daoProperties;

    /**
     * Custom projects dao connection reader
     */
    @Autowired(required = false)
    private IMultitenantConnectionsReader customProjectsConnectionReader;

    /**
     *
     * List of data sources for each configured tenant.
     *
     * @return Map<Tenant, DataSource>
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = { "multitenantsDataSources" })
    public Map<String, DataSource> getDataSources() {

        final Map<String, DataSource> datasources = new HashMap<>();

        // Add datasources from bean configuration
        if (customProjectsConnectionReader != null) {
            final List<ProjectConnection> connections = customProjectsConnectionReader.getTenantConnections();
            for (final ProjectConnection connection : connections) {
                final Project project = connection.getProject();
                if (!datasources.containsKey(project.getName())) {
                    if (daoProperties.getEmbedded()) {
                        datasources.put(project.getName(), DataSourceHelper
                                .createEmbeddedDataSource(project.getName(), daoProperties.getEmbeddedPath()));
                    } else {
                        datasources.put(project.getName(),
                                        DataSourceHelper
                                                .createDataSource(connection.getUrl(), connection.getDriverClassName(),
                                                                  connection.getUserName(), connection.getPassword()));
                    }
                } else {
                    LOG.warn(String.format("Datasource for project %s already defined.", project.getName()));
                }
            }
        } else {
            LOG.warn("No Custom tenant reader defined. Using only properties file to create datasources for MultitenantJpaAutoConfiguration");
        }

        // Add datasources configuration from properties file.
        for (final TenantConfiguration tenant : daoProperties.getTenants()) {
            DataSource datasource = null;
            if (daoProperties.getEmbedded()) {
                datasource = DataSourceHelper.createEmbeddedDataSource(tenant.getName(),
                                                                       daoProperties.getEmbeddedPath());

            } else {
                datasource = DataSourceHelper
                        .createDataSource(tenant.getDatasource().getUrl(), tenant.getDatasource().getDriverClassName(),
                                          tenant.getDatasource().getUsername(), tenant.getDatasource().getPassword());
            }
            if (!datasources.containsKey(tenant.getName())) {
                datasources.put(tenant.getName(), datasource);
            } else {
                LOG.warn(String.format("Datasource for project %s already defined.", tenant.getName()));
            }
        }

        return datasources;
    }

    /**
     *
     * Default data source for persistence unit projects.
     *
     * ConditionalOnMissingBean : In case of jpa-instance-regards-starter activated. There can't be two datasources.
     *
     * @return datasource
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource projectsDataSource() {
        final Map<String, DataSource> multitenantsDataSources = getDataSources();
        DataSource datasource = null;
        if ((multitenantsDataSources != null) && !multitenantsDataSources.isEmpty()) {
            datasource = multitenantsDataSources.values().iterator().next();
        } else {
            LOG.error("No datasource defined for MultitenantcyJpaAutoConfiguration !");
        }
        return datasource;
    }
}
