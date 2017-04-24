/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.utils.TenantDataSourceHelper;

/**
 *
 * Configuration class to define the default PostgresSQL Data base
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableConfigurationProperties(MultitenantDaoProperties.class)
@AutoConfigureAfter(value = { AmqpAutoConfiguration.class })
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class DataSourcesAutoConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourcesAutoConfiguration.class);

    /**
     * Data source registry bean
     */
    public static final String DATA_SOURCE_BEAN_NAME = "multitenantsDataSources";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Microservice globale configuration
     */
    @Autowired
    private MultitenantDaoProperties daoProperties;

    /**
     * Custom projects dao connection reader
     */
    @Autowired
    private ITenantConnectionResolver tenantConnectionResolver;

    /**
     *
     * List of data sources for each configured tenant.
     *
     * @return Map<Tenant, DataSource>
     * @throws JpaMultitenantException
     *             if connections cannot be retrieved on startup
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = { DATA_SOURCE_BEAN_NAME })
    public Map<String, DataSource> getDataSources() throws JpaMultitenantException {

        Map<String, DataSource> datasources = new HashMap<>();

        // Retrieve microservice tenant connections from multitenant resolver
        List<TenantConnection> connections = tenantConnectionResolver.getTenantConnections(microserviceName);
        // Initialize tenant connections
        initDataSources(datasources, connections, false);
        // Add static datasource configuration from properties file if necessary
        initDataSources(datasources, daoProperties.getTenants(), true);

        return datasources;
    }

    /**
     *
     * Create the datasources from the TenantConfiguration list given
     *
     * @param pExistingDataSources
     *            list of existing datasources
     * @param pConnections
     *            pTenants tenants configuration
     * @param pNeedRegistration
     *            if data source have to be registered
     * @return datasources created
     * @since 1.0-SNAPSHOT
     */
    private void initDataSources(Map<String, DataSource> pExistingDataSources,
            final List<TenantConnection> pConnections, boolean pNeedRegistration) {

        for (final TenantConnection tenantConnection : pConnections) {

            // Prevent duplicates
            if (!pExistingDataSources.containsKey(tenantConnection.getTenant())) {

                try {
                    // Init data source
                    DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties, tenantConnection);
                    // Register connection
                    if (pNeedRegistration) {
                        tenantConnectionResolver.addTenantConnection(microserviceName, tenantConnection);
                    }
                    // Register data source
                    pExistingDataSources.put(tenantConnection.getTenant(), dataSource);
                } catch (PropertyVetoException | JpaMultitenantException e) {
                    // Do not block all tenants if for an inconsistent data source
                    LOGGER.error("Cannot create datasource for tenant {}", tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }

            } else {
                LOGGER.warn(String.format("Datasource for tenant %s already defined.", tenantConnection.getTenant()));
            }
        }
    }

    /**
     *
     * Default data source for persistence unit projects.
     *
     * ConditionalOnMissingBean : In case of jpa-instance-regards-starter activated. There can't be two datasources.
     *
     * @return datasource
     * @throws JpaMultitenantException
     *             if connections cannot be retrieved on startup
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource projectsDataSource() throws JpaMultitenantException {
        final Map<String, DataSource> multitenantsDataSources = getDataSources();
        DataSource datasource = null;
        if ((multitenantsDataSources != null) && !multitenantsDataSources.isEmpty()) {
            datasource = multitenantsDataSources.values().iterator().next();
        } else {
            LOGGER.error("No datasource defined for MultitenantcyJpaAutoConfiguration !");
        }
        return datasource;
    }
}
