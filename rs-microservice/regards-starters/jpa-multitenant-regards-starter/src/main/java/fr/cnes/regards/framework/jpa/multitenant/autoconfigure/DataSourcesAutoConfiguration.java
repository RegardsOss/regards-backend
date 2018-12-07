/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherAutoConf;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.JpaException;
import fr.cnes.regards.framework.jpa.multitenant.event.MultitenantJpaEventHandler;
import fr.cnes.regards.framework.jpa.multitenant.event.MultitenantJpaEventPublisher;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DefaultTenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.utils.TenantDataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.framework.jpa.utils.Hbm2ddlDatasourceSchemaHelper;
import fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper;
import fr.cnes.regards.framework.jpa.utils.MigrationTool;

/**
 * Configuration class to define the default PostgresSQL Data base
 * @author Sébastien Binda
 */
@Configuration
@EnableConfigurationProperties(MultitenantDaoProperties.class)
@AutoConfigureAfter(value = { AmqpAutoConfiguration.class, CipherAutoConf.class })
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class DataSourcesAutoConfiguration {

    /**
     * Data source registry bean
     */
    public static final String DATA_SOURCE_BEAN_NAME = "multitenantsDataSources";

    /**
     * {@link IDatasourceSchemaHelper} instance bean
     */
    public static final String DATASOURCE_SCHEMA_HELPER_BEAN_NAME = "multitenantDataSourceSchemaHelper";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourcesAutoConfiguration.class);

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${spring.jpa.hibernate.naming.implicit-strategy:org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}")
    private String implicitNamingStrategyName;

    @Value("${spring.jpa.hibernate.naming.physical-strategy:org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl}")
    private String physicalNamingStrategyName;

    /**
     * Microservice global configuration
     */
    @Autowired
    private MultitenantDaoProperties daoProperties;

    /**
     * JPA Configuration
     */
    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private HibernateProperties hb8Properties;

    @Autowired
    private IEncryptionService encryptionService;

    /**
     * Create a default TenantConnection resolver if none defined.
     * @return ITenantConnectionResolver
     */
    @Bean
    @ConditionalOnMissingBean
    public ITenantConnectionResolver defaultTenantConnectionResolver() {
        return new DefaultTenantConnectionResolver();
    }

    /**
     * List of data sources for each configured tenant.
     * @return Map { Tenant, DataSource }
     * @throws JpaMultitenantException if connections cannot be retrieved on startup
     */
    @Bean(name = { DATA_SOURCE_BEAN_NAME })
    public Map<String, DataSource> getDataSources(ITenantConnectionResolver tenantConnectionResolver)
            throws JpaMultitenantException, EncryptionException {
        Map<String, DataSource> datasources = new HashMap<>();
        // Retrieve microservice tenant connections from multitenant resolver
        List<TenantConnection> connections = tenantConnectionResolver.getTenantConnections(microserviceName);
        // Initialize tenant connections
        // First lets decrypt connections password
        for (TenantConnection connection : connections) {
            connection.setPassword(encryptionService.decrypt(connection.getPassword()));
        }
        initDataSources(datasources, connections, false, tenantConnectionResolver);
        // Add static datasources configuration from properties file if necessary
        // configuration files are not encrypted so we do not need to decrypt passwords here.
        initDataSources(datasources, daoProperties.getTenants(), true, tenantConnectionResolver);

        return datasources;
    }

    /**
     * Initialize programmatic schema update helper
     * @return {@link IDatasourceSchemaHelper}
     * @throws JpaException if error occurs!
     */
    @Bean(name = DATASOURCE_SCHEMA_HELPER_BEAN_NAME)
    public IDatasourceSchemaHelper datasourceSchemaHelper() throws JpaException {
        Map<String, Object> hibernateProperties = getHibernateProperties();
        if (MigrationTool.HBM2DDL.equals(daoProperties.getMigrationTool())) {
            Hbm2ddlDatasourceSchemaHelper helper = new Hbm2ddlDatasourceSchemaHelper(hibernateProperties, Entity.class,
                    InstanceEntity.class);
            // Set output file, may be null.
            helper.setOutputFile(daoProperties.getOutputFile());
            return helper;
        } else {
            return new FlywayDatasourceSchemaHelper(hibernateProperties);
        }
    }

    /**
     * Init JPA event handler manager
     * @param instanceSubscriber to subscribe to tenant connection events
     * @param multitenantResolver to resolve tenant
     * @return JPA event handler
     * @throws JpaMultitenantException if error occurs!
     */
    @Bean
    public MultitenantJpaEventHandler multitenantJpaEventHandler(IInstanceSubscriber instanceSubscriber,
            ITenantConnectionResolver multitenantResolver,
            @Qualifier(DATASOURCE_SCHEMA_HELPER_BEAN_NAME) IDatasourceSchemaHelper datasourceSchemaHelper,
            @Qualifier(DataSourcesAutoConfiguration.DATA_SOURCE_BEAN_NAME) Map<String, DataSource> dataSources) {
        return new MultitenantJpaEventHandler(microserviceName, dataSources, daoProperties, datasourceSchemaHelper,
                instanceSubscriber, multitenantResolver, localPublisher(), encryptionService);
    }

    /**
     * Spring managed events for informing all microservice modules
     * @return {@link MultitenantJpaEventPublisher}
     */
    @Bean
    public MultitenantJpaEventPublisher localPublisher() {
        return new MultitenantJpaEventPublisher();
    }

    /**
     * Create the datasources from the TenantConfiguration list given
     * @param existingDataSources list of existing datasources
     * @param connections pTenants tenants configuration
     * @param needRegistration if data source have to be registered
     * @return datasources created
     */
    private void initDataSources(Map<String, DataSource> existingDataSources, List<TenantConnection> connections,
            boolean needRegistration, ITenantConnectionResolver tenantConnectionResolver) {

        for (TenantConnection tenantConnection : connections) {
            // Prevent duplicates
            if (!existingDataSources.containsKey(tenantConnection.getTenant())) {
                try {
                    // Init data source
                    DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties, tenantConnection);
                    // Update database schema
                    datasourceSchemaHelper().migrate(dataSource);
                    // Register connection
                    if (needRegistration) {
                        tenantConnectionResolver.addTenantConnection(microserviceName, tenantConnection);
                    }
                    // Register data source
                    existingDataSources.put(tenantConnection.getTenant(), dataSource);
                } catch (PropertyVetoException | JpaMultitenantException | JpaException | SQLException
                        | IOException e) {
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
     * Default data source for persistence unit projects.
     *
     * ConditionalOnMissingBean : In case of jpa-instance-regards-starter activated. There can't be two datasources.
     * @return datasource
     * @throws JpaMultitenantException if connections cannot be retrieved on startup
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource projectsDataSource(
            @Qualifier(DataSourcesAutoConfiguration.DATA_SOURCE_BEAN_NAME) Map<String, DataSource> dataSources) {
        DataSource datasource = null;
        if (dataSources != null && !dataSources.isEmpty()) {
            datasource = dataSources.values().iterator().next();
        } else {
            LOGGER.error("No datasource defined for MultitenantJpaAutoConfiguration !");
        }
        return datasource;
    }

    /**
     * Compute database properties
     * @return database properties
     * @throws JpaException if error occurs!
     */
    private Map<String, Object> getHibernateProperties() throws JpaException {

        // Add Spring JPA hibernate properties
        // Schema must be retrieved here if managed with property :
        // spring.jpa.properties.hibernate.default_schema
        // Before retrieving hibernate properties, set ddl auto to avoid the need of a datasource
        hb8Properties.setDdlAuto("none");
        Map<String, Object> dbProperties = new HashMap<>(
                hb8Properties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings()));
        // Remove hbm2ddl as schema update is done programmatically
        dbProperties.remove(Environment.HBM2DDL_AUTO);

        // Dialect
        String dialect = daoProperties.getDialect();
        if (daoProperties.getEmbedded()) {
            // Force dialect for embedded database
            dialect = DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT;
        }
        dbProperties.put(Environment.DIALECT, dialect);

        dbProperties.put(Environment.USE_NEW_ID_GENERATOR_MAPPINGS, true);

        try {
            PhysicalNamingStrategy hibernatePhysicalNamingStrategy = (PhysicalNamingStrategy) Class
                    .forName(physicalNamingStrategyName).newInstance();
            dbProperties.put(Environment.PHYSICAL_NAMING_STRATEGY, hibernatePhysicalNamingStrategy);
            final ImplicitNamingStrategy hibernateImplicitNamingStrategy = (ImplicitNamingStrategy) Class
                    .forName(implicitNamingStrategyName).newInstance();
            dbProperties.put(Environment.IMPLICIT_NAMING_STRATEGY, hibernateImplicitNamingStrategy);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOGGER.error("Error occurs with naming strategy", e);
            throw new JpaException(e);
        }
        return dbProperties;
    }
}
