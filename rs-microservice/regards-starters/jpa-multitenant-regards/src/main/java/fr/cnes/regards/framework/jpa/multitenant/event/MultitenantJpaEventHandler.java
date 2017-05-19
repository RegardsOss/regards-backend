/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.exception.JpaException;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.utils.TenantDataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper;

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

    /**
     * Schema update helper
     */
    private final IDatasourceSchemaHelper datasourceSchemaHelper;

    public MultitenantJpaEventHandler(String microserviceName, Map<String, DataSource> dataSources,
            MultitenantDaoProperties daoProperties, IDatasourceSchemaHelper datasourceSchemaHelper,
            IInstanceSubscriber instanceSubscriber, IInstancePublisher instancePublisher,
            ITenantConnectionResolver multitenantResolver) {
        this.microserviceName = microserviceName;
        this.dataSources = dataSources;
        this.daoProperties = daoProperties;
        this.datasourceSchemaHelper = datasourceSchemaHelper;
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
     * Test connection
     *
     * @param dataSource
     *            related data source
     * @throws JpaMultitenantException
     *             if connection fails
     */
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
                datasourceSchemaHelper.migrate(dataSource);
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
            } catch (JpaException e) {
                LOGGER.error("Cannot migrate datasource for tenant {}. Update fails.", tenantConnection.getTenant());
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
