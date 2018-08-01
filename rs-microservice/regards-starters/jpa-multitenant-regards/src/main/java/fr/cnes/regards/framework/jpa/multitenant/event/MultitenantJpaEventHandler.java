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
package fr.cnes.regards.framework.jpa.multitenant.event;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import com.mchange.v2.c3p0.DataSources;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
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
     * Spring events for local events broadcasting
     */
    private final MultitenantJpaEventPublisher localPublisher;

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
            IInstanceSubscriber instanceSubscriber, ITenantConnectionResolver multitenantResolver,
            MultitenantJpaEventPublisher localPublisher) {
        this.microserviceName = microserviceName;
        this.dataSources = dataSources;
        this.daoProperties = daoProperties;
        this.datasourceSchemaHelper = datasourceSchemaHelper;
        this.instanceSubscriber = instanceSubscriber;
        this.multitenantResolver = multitenantResolver;
        this.localPublisher = localPublisher;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
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
                // Trying to connect data source
                multitenantResolver.updateState(microserviceName, tenantConnection.getTenant(),
                                                TenantConnectionState.CONNECTING, Optional.empty());
                // Init data source
                DataSource dataSource = TenantDataSourceHelper.initDataSource(daoProperties, tenantConnection);
                // Remove existing one
                DataSource oldDataSource = dataSources.remove(tenantConnection.getTenant());
                if (oldDataSource != null) {
                    DataSources.destroy(oldDataSource);
                }
                // Update schema
                datasourceSchemaHelper.migrate(dataSource);
                // Enable data source
                multitenantResolver.updateState(microserviceName, tenantConnection.getTenant(),
                                                TenantConnectionState.ENABLED, Optional.empty());
                // Register data source
                dataSources.put(tenantConnection.getTenant(), dataSource);
                // Broadcast connection ready with a Spring event
                localPublisher.publishConnectionReady(tenantConnection.getTenant());
            } catch (PropertyVetoException | SQLException e) {
                LOGGER.error("Cannot handle tenant connection for project {} and microservice {}",
                             tenantConnection.getTenant(), eventMicroserviceName);
                LOGGER.error("Exception occurs", e);
                try {
                    multitenantResolver.updateState(microserviceName, tenantConnection.getTenant(),
                                                    TenantConnectionState.ERROR, Optional.ofNullable(e.getMessage()));
                } catch (JpaMultitenantException e1) { // NOSONAR do no propagate error and try to init other
                                                       // connections
                    LOGGER.error("Cannot update datasource for tenant {}. Update fails.", tenantConnection.getTenant());
                    LOGGER.error(e.getMessage(), e);
                }
            } catch (JpaMultitenantException e) {
                LOGGER.error("Cannot enable datasource for project {} and microservice {}. Update fails.",
                             tenantConnection.getTenant(), eventMicroserviceName);
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
                        DataSources.destroy(oldDataSource);
                    }
                    // Broadcast connection discarded with a Spring event
                    localPublisher.publishConnectionDiscarded(tenantConnection.getTenant());
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
                        DataSources.destroy(oldDataSource);
                    }
                    // Disable connection
                    multitenantResolver.updateState(microserviceName, tcf.getTenant(), TenantConnectionState.ERROR,
                                                    Optional.of("Connection failed event received!"));
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
