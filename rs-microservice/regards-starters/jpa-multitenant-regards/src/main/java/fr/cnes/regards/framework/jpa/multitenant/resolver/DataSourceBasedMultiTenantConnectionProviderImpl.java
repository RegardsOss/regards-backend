/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationCreated;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationDeleted;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationUpdated;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionDiscarded;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.exception.InvalidDataSourceTenant;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.utils.TenantDataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 *
 * Multitenancy Database Connection Provider. By default only one connection is available. The one defined in the
 * DataSourceConfig class. To add a new connection use the addDataSource method
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@SuppressWarnings("serial")
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DataSourceBasedMultiTenantConnectionProviderImpl.class);

    /**
     * Current microservice name
     */
    private String microserviceName;

    /**
     * AMQP Message subscriber
     */
    private transient IInstanceSubscriber instanceSubscriber;

    /**
     * AMQP Message subscriber
     */
    private transient IInstancePublisher instancePublisher;

    /**
     * Microservice global configuration
     */
    private transient MultitenantDaoProperties daoProperties;

    /**
     * Pool of datasources available for this connection provider
     */
    private transient Map<String, DataSource> dataSources;

    public DataSourceBasedMultiTenantConnectionProviderImpl(final MultitenantDaoProperties pDaoProperties,
            final Map<String, DataSource> pDataSources, final IInstanceSubscriber pAmqpSubscriber,
            final String pMicroserviceName) {
        super();
        daoProperties = pDaoProperties;
        dataSources = pDataSources;
        instanceSubscriber = pAmqpSubscriber;
        microserviceName = pMicroserviceName;
    }

    public DataSourceBasedMultiTenantConnectionProviderImpl(final MultitenantDaoProperties pDaoProperties) {
        super();
        daoProperties = pDaoProperties;
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSources.values().iterator().next();
    }

    @Override
    protected DataSource selectDataSource(final String pTenantIdentifier) {
        DataSource tenantDataSource = dataSources.get(pTenantIdentifier);
        if (tenantDataSource == null) {
            String message = String.format("No data source found for tenant %s.", pTenantIdentifier);
            LOGGER.error(message);
            throw new InvalidDataSourceTenant(message);
        }
        return dataSources.get(pTenantIdentifier);
    }

    /**
     *
     * Initialize configured datasources
     *
     * @since 1.0-SNAPSHOT
     */
    @PostConstruct
    public void initDataSources() {
        // Hibernate do not initialize schema for multitenants.
        // Here whe manually update schema for all configured datasources
        for (final String tenant : dataSources.keySet()) {
            updateDataSourceSchema(selectDataSource(tenant));
        }

        // Listen for tenant connection creation
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationCreated.class, new ConfigurationCreatedHandler());
        // Listen for tenant connection update
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationUpdated.class, new ConfigurationUpdatedHandler());
        // Listen for tenant connection deletion
        instanceSubscriber.subscribeTo(TenantConnectionConfigurationDeleted.class, new ConfigurationDeletedHandler());
    }

    /**
     *
     * Update datasource schema
     *
     * @param pDataSource
     *            datasource to update
     * @since 1.0-SNAPSHOT
     */
    private void updateDataSourceSchema(final DataSource pDataSource) {
        // 1. Update database schema if needed
        String dialect = daoProperties.getDialect();
        if (daoProperties.getEmbedded()) {
            dialect = DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT;
        }
        final MetadataSources metadata = new MetadataSources(new StandardServiceRegistryBuilder()
                .applySetting(Environment.DIALECT, dialect).applySetting(Environment.DATASOURCE, pDataSource).build());

        Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        packagesToScan.stream()
                .flatMap(pPackage -> DaoUtils.scanPackageForJpa(pPackage, Entity.class, InstanceEntity.class).stream())
                .forEach(classe -> metadata.addAnnotatedClass(classe));

        final SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true);
    }

    /**
     * Handle {@link TenantConnection} configuration creation
     *
     * @author Sébastien Binda
     */
    private class ConfigurationCreatedHandler implements IHandler<TenantConnectionConfigurationCreated> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionConfigurationCreated> pEvent) {
            // Add a new datasource to the current pool of datasource if the current microservice is the target of the
            // new
            // tenant connection
            if ((pEvent.getContent() != null) && microserviceName.equals(pEvent.getContent().getMicroserviceName())) {
                final TenantConnection tenantConnection = pEvent.getContent().getTenant();
                try {
                    // Init data source
                    DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties, tenantConnection);
                    // Register data source
                    dataSources.put(tenantConnection.getTenant(), dataSource);
                    // Update schema
                    updateDataSourceSchema(dataSource);
                    // Broadcast connection ready
                    instancePublisher.publish(new TenantConnectionReady(tenantConnection.getTenant(),
                            pEvent.getContent().getMicroserviceName()));
                } catch (PropertyVetoException e) {
                    // Do not block all tenants if for an inconsistent data source
                    LOGGER.error("Cannot handle datasource for tenant {}. Creation fails.",
                                 tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
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

            if ((pEvent.getContent() != null) && microserviceName.equals(pEvent.getContent().getMicroserviceName())) {
                final TenantConnection tenantConnection = pEvent.getContent().getTenant();
                try {
                    // Init data source
                    DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties, tenantConnection);
                    // Remove existing one
                    DataSource oldDataSource = dataSources.remove(tenantConnection.getTenant());
                    if (oldDataSource != null) {
                        oldDataSource.getConnection().close();
                    }
                    // Register updated data source
                    dataSources.put(tenantConnection.getTenant(), dataSource);
                    // Update schema
                    updateDataSourceSchema(dataSource);
                    // Broadcast connection ready
                    instancePublisher.publish(new TenantConnectionReady(tenantConnection.getTenant(),
                            pEvent.getContent().getMicroserviceName()));
                } catch (PropertyVetoException e) {
                    // Do not block all tenants if for an inconsistent data source
                    LOGGER.error("Cannot handle datasource for tenant {}. Update fails.", tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                } catch (SQLException e) {
                    LOGGER.error("Cannot release datasource for tenant {}. Update fails while closing old connection.",
                                 tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
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
}
