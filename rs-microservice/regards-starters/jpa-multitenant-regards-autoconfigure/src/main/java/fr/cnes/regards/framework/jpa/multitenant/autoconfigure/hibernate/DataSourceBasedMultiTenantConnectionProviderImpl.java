/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.hibernate;

import java.util.HashMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
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
@Component
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final long serialVersionUID = 8168907057647334460L;

    /**
     * Microservice global configuration
     */
    @Autowired
    private transient MultitenantDaoProperties daoProperties;

    /**
     * Pool of datasources available for this connection provider
     */
    @Autowired
    @Qualifier("multitenantsDataSources")
    private final transient Map<String, DataSource> dataSources = new HashMap<>();

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
     * @since 1.0-SNAPSHOT
     */
    @PostConstruct
    public void initDataSources() {
        // Hibernate do not initialize schema for multitenants.
        // Here whe manually update schema for all configured datasources
        for (final String tenant : dataSources.keySet()) {
            updateDataSourceSchema(selectDataSource(tenant));
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
