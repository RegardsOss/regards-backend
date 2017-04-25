/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.utils.TenantDataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 *
 * This class manages JPA event workflow.
 *
 * @author Marc Sordi
 *
 */
public class MultitenantJpaEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantJpaEventHandler.class);

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * Database schema name
     */
    private final String schemaName;

    /**
     * Implicit naming strategy
     */
    private final String implicitNamingStrategyName;

    /**
     * Physical naming strategy
     */
    private final String physicalNamingStrategyName;

    /**
     * AMQP Message subscriber
     */
    private final IInstanceSubscriber instanceSubscriber;

    /**
     * AMQP Message subscriber
     */
    private final IInstancePublisher instancePublisher;

    /**
     * Custom projects dao connection reader
     */
    private final ITenantConnectionResolver multitenantResolver;

    /**
     * Pool of datasources available for this connection provider
     */
    private final Map<String, DataSource> dataSources;

    /**
     * Microservice global configuration
     */
    private final MultitenantDaoProperties daoProperties;

    public MultitenantJpaEventHandler(String microserviceName, String schemaName, Map<String, DataSource> dataSources,
            MultitenantDaoProperties daoProperties, String implicitNamingStrategyName,
            String physicalNamingStrategyName, IInstanceSubscriber instanceSubscriber,
            IInstancePublisher instancePublisher, ITenantConnectionResolver multitenantResolver) {
        this.microserviceName = microserviceName;
        this.schemaName = schemaName;
        this.dataSources = dataSources;
        this.daoProperties = daoProperties;
        this.implicitNamingStrategyName = implicitNamingStrategyName;
        this.physicalNamingStrategyName = physicalNamingStrategyName;
        this.instancePublisher = instancePublisher;
        this.instanceSubscriber = instanceSubscriber;
        this.multitenantResolver = multitenantResolver;
    }

    /*
     *
     * (non-Javadoc)
     *
     * @see
     * org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        // Listen to tenant connection creation
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationCreated.class, new ConfigurationCreatedHandler());
        // Listen to tenant connection update
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationUpdated.class, new ConfigurationUpdatedHandler());
        // Listen to tenant connection deletion
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationDeleted.class, new ConfigurationDeletedHandler());
        // Listen to tenant connection failure
        instanceSubscriber.subscribeTo(TenantConnectionFailed.class, new TenantConnectionFailedHandler());
    }

    /**
     *
     * Update datasource schema
     *
     * @param pDataSource
     *            datasource to update
     * @since 1.0-SNAPSHOT
     */
    private void updateDataSourceSchema(final DataSource pDataSource) throws JpaMultitenantException {
        // 1. Update database schema if needed
        String dialect = daoProperties.getDialect();
        if (daoProperties.getEmbedded()) {
            dialect = DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT;
        }

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        builder.applySetting(Environment.DIALECT, dialect);
        builder.applySetting(Environment.DATASOURCE, pDataSource);
        builder.applySetting(Environment.USE_NEW_ID_GENERATOR_MAPPINGS, true);
        builder.applySetting(Environment.DEFAULT_SCHEMA, schemaName);

        try {
            PhysicalNamingStrategy hibernatePhysicalNamingStrategy = (PhysicalNamingStrategy) Class
                    .forName(physicalNamingStrategyName).newInstance();
            builder.applySetting(Environment.PHYSICAL_NAMING_STRATEGY, hibernatePhysicalNamingStrategy);
            final ImplicitNamingStrategy hibernateImplicitNamingStrategy = (ImplicitNamingStrategy) Class
                    .forName(implicitNamingStrategyName).newInstance();
            builder.applySetting(Environment.IMPLICIT_NAMING_STRATEGY, hibernateImplicitNamingStrategy);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOGGER.error("Error occurs with naming strategy", e);
            throw new JpaMultitenantException(e);
        }

        final MetadataSources metadata = new MetadataSources(builder.build());

        Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        packagesToScan.stream()
                .flatMap(pPackage -> DaoUtils.scanPackageForJpa(pPackage, Entity.class, InstanceEntity.class).stream())
                .forEach(metadata::addAnnotatedClass);

        final SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true);
    }

    private void testConnection(DataSource dataSource) throws JpaMultitenantException {
        try (Connection connection = dataSource.getConnection()) {
            LOGGER.debug("Successful data source connection test");
        } catch (SQLException e) {
            LOGGER.debug("Data source connection fails.", e);
            throw new JpaMultitenantException(e);
        }
    }

    /**
     * Handle a new {@link TenantConnection} if the related microservice is exactly the same.
     *
     * @param eventMicroserviceName
     *            related microservice
     * @param tenantConnection
     *            {@link TenantConnection} for the microservice
     */
    private void handleTenantConnection(String eventMicroserviceName, TenantConnection tenantConnection) {
        if (microserviceName.equals(eventMicroserviceName)) {
            try {
                // Init data source
                DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties, tenantConnection);
                // Test data source
                testConnection(dataSource);
                // Remove existing one
                DataSource oldDataSource = dataSources.remove(tenantConnection.getTenant());
                if (oldDataSource != null) {
                    oldDataSource.getConnection().close();
                }
                // Update schema
                updateDataSourceSchema(dataSource);
                // Enable data source
                multitenantResolver.enableTenantConnection(microserviceName, tenantConnection.getTenant());
                // Register data source
                dataSources.put(tenantConnection.getTenant(), dataSource);
                // Broadcast connection ready
                instancePublisher
                        .publish(new TenantConnectionReady(tenantConnection.getTenant(), eventMicroserviceName));
            } catch (PropertyVetoException e) {
                // Do not block all tenants if for an inconsistent data source
                LOGGER.error("Cannot handle datasource for tenant {}. Creation fails.", tenantConnection.getTenant());
                LOGGER.error(e.getMessage(), e);
            } catch (SQLException e) {
                LOGGER.error("Cannot release datasource for tenant {}. Update fails while closing old connection.",
                             tenantConnection.getTenant());
                LOGGER.error(e.getMessage(), e);
            } catch (JpaMultitenantException e) {
                LOGGER.error("Cannot enable datasource for tenant {}. Update fails.", tenantConnection.getTenant());
                LOGGER.error(e.getMessage(), e);
            } catch (HibernateException e) {
                // An error may occurs when update schema
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle {@link TenantConnection} configuration creation
     *
     * @author Sébastien Binda
     */
    private class ConfigurationCreatedHandler implements IHandler<TenantConnectionConfigurationCreated> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionConfigurationCreated> pEvent) {
            if (pEvent.getContent() != null) {
                handleTenantConnection(pEvent.getContent().getMicroserviceName(), pEvent.getContent().getTenant());
            }
        }
    }

    /**
     * Handle {@link TenantConnection} configuration update
     *
     * @author Marc Sordi
     *
     */
    private class ConfigurationUpdatedHandler implements IHandler<TenantConnectionConfigurationUpdated> {

        @Override
        public void handle(TenantWrapper<TenantConnectionConfigurationUpdated> pEvent) {
            if (pEvent.getContent() != null) {
                handleTenantConnection(pEvent.getContent().getMicroserviceName(), pEvent.getContent().getTenant());
            }
        }
    }

    /**
     * Handle {@link TenantConnection} configuration deletion
     *
     * @author Marc Sordi
     *
     */
    private class ConfigurationDeletedHandler implements IHandler<TenantConnectionConfigurationDeleted> {

        @Override
        public void handle(TenantWrapper<TenantConnectionConfigurationDeleted> pEvent) {

            if ((pEvent.getContent() != null) && microserviceName.equals(pEvent.getContent().getMicroserviceName())) {
                final TenantConnection tenantConnection = pEvent.getContent().getTenant();
                try {
                    // Remove existing datasource
                    DataSource oldDataSource = dataSources.remove(tenantConnection.getTenant());
                    if (oldDataSource != null) {
                        oldDataSource.getConnection().close();
                    }
                    // Broadcast connection ready
                    instancePublisher.publish(new TenantConnectionDiscarded(tenantConnection.getTenant(),
                            pEvent.getContent().getMicroserviceName()));
                } catch (SQLException e) {
                    LOGGER.error("Cannot release datasource for tenant {}. Delete fails while closing existing connection.",
                                 tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Handle {@link TenantConnection} fail event
     *
     * @author Marc Sordi
     *
     */
    private class TenantConnectionFailedHandler implements IHandler<TenantConnectionFailed> {

        @Override
        public void handle(TenantWrapper<TenantConnectionFailed> pEvent) {

            if ((pEvent.getContent() != null) && microserviceName.equals(pEvent.getContent().getMicroserviceName())) {
                final TenantConnectionFailed tcf = pEvent.getContent();
                try {
                    // Remove existing datasource
                    DataSource oldDataSource = dataSources.remove(tcf.getTenant());
                    if (oldDataSource != null) {
                        oldDataSource.getConnection().close();
                    }
                    // Disable connection
                    multitenantResolver.disableTenantConnection(tcf.getMicroserviceName(), tcf.getTenant());
                } catch (SQLException e) {
                    LOGGER.error("Cannot release datasource for tenant {}. Cannot close connection", tcf.getTenant());
                    LOGGER.error(e.getMessage(), e);
                } catch (JpaMultitenantException e) {
                    LOGGER.error("Cannot disable datasource for tenant {}. Update fails.", tcf.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

}
