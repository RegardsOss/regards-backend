/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.event.NewTenantEvent;
import fr.cnes.regards.framework.jpa.multitenant.event.handler.NewTenantHandler;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
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
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 8168907057647334460L;

    /**
     * Current microservice name
     */
    private String microserviceName;

    /**
     * AMQP Message subscriber
     */
    private transient Subscriber amqpSubscriber;

    /**
     * Microservice global configuration
     */
    private transient MultitenantDaoProperties daoProperties;

    /**
     * Pool of datasources available for this connection provider
     */
    private transient Map<String, DataSource> dataSources;

    public DataSourceBasedMultiTenantConnectionProviderImpl(final MultitenantDaoProperties pDaoProperties,
            final Map<String, DataSource> pDataSources, final Subscriber pAmqpSubscriber,
            final String pMicroserviceName) {
        super();
        daoProperties = pDaoProperties;
        dataSources = pDataSources;
        amqpSubscriber = pAmqpSubscriber;
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
        return dataSources.get(pTenantIdentifier);
    }

    /**
     *
     * Initialize configured datasources
     *
     * @throws RabbitMQVhostException
     *             Unrecoverable error during AMQP initialization
     *
     * @since 1.0-SNAPSHOT
     */
    @PostConstruct
    public void initDataSources() throws RabbitMQVhostException {
        // Hibernate do not initialize schema for multitenants.
        // Here whe manually update schema for all configured datasources
        for (final String tenant : dataSources.keySet()) {
            updateDataSourceSchema(selectDataSource(tenant));
        }

        listenForNewTenant();
    }

    /**
     *
     * Add a listener to all tenant creation event. If a new tenant is created, we have to dynamically add the
     * dataSource to the connection pool
     *
     * @throws RabbitMQVhostException
     *             Unrecoverable error during AMQP initialization
     * @since 1.0-SNAPSHOT
     */
    private void listenForNewTenant() throws RabbitMQVhostException {
        if (amqpSubscriber != null) {
            final IHandler<NewTenantEvent> tenantHandler = new NewTenantHandler(this, microserviceName);
            amqpSubscriber.subscribeTo(NewTenantEvent.class, tenantHandler, AmqpCommunicationMode.ONE_TO_MANY,
                                       AmqpCommunicationTarget.EXTERNAL);
            amqpSubscriber.subscribeTo(NewTenantEvent.class, tenantHandler, AmqpCommunicationMode.ONE_TO_ONE,
                                       AmqpCommunicationTarget.INTERNAL);
        }
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

        // 2 Add Entity for database mapping from classpath
        final List<Class<?>> classes = DaoUtils.scanForJpaPackages(DaoUtils.PACKAGES_TO_SCAN, Entity.class,
                                                                   InstanceEntity.class);
        classes.forEach(classe -> metadata.addAnnotatedClass(classe));

        final SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true);
    }

    /**
     *
     * Add a datasource to the multitenant datasources pool
     *
     * @param pDataSource
     *            : Datasource to add
     * @param pTenant
     *            : tenant name
     * @since 1.0-SNAPSHOT
     */
    private void addDataSource(final DataSource pDataSource, final String pTenant) {
        dataSources.put(pTenant, pDataSource);
        updateDataSourceSchema(pDataSource);
    }

    /**
     *
     * Create a new datasource into the multitenant datasources pool
     *
     * @param pUrl
     *            : Only used if the dao is not configured in embedded mod
     * @param pUser
     *            : Only used if the dao is not configured in embedded mod
     * @param pPassword
     *            : Only used if the dao is not configured in embedded mod
     * @param pDriverClassName
     *            : Only used if the dao is not configured in embedded mod
     * @param pTenant
     *            : tenant
     * @since 1.0-SNAPSHOT
     */
    public void addDataSource(final String pUrl, final String pUser, final String pPassword,
            final String pDriverClassName, final String pTenant) {

        if (daoProperties.getEmbedded()) {
            addDataSource(DataSourceHelper.createEmbeddedDataSource(pTenant, daoProperties.getEmbeddedPath()), pTenant);
        } else {
            addDataSource(DataSourceHelper.createDataSource(pUrl, pDriverClassName, pUser, pPassword), pTenant);
        }
    }

}
