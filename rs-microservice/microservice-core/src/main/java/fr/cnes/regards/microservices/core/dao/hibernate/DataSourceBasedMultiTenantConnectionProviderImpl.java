/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.hibernate;

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
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.configuration.common.ProjectConfiguration;
import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;
import fr.cnes.regards.microservices.core.dao.jpa.DaoUtils;
import fr.cnes.regards.microservices.core.dao.jpa.DataSourcesConfiguration;

/**
 *
 * Multitenancy Database Connection Provider. By default only one connection is available. The one defined in the
 * DataSourceConfig class. To add a new connection use the addDataSource method
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
@ConditionalOnProperty("microservice.dao.enabled")
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final long serialVersionUID = 8168907057647334460L;

    @Autowired
    private transient MicroserviceConfiguration configuration_;

    /**
     * Pool of datasources available for this connection provider
     */
    @Autowired
    @Qualifier("dataSources")
    private final transient Map<String, DataSource> dataSources_ = new HashMap<>();

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSources_.get(configuration_.getProjects().get(0).getName());
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return dataSources_.get(tenantIdentifier);
    }

    @PostConstruct
    public void initDataSources() {
        // Hibernate do not initialize schema for multitenants.
        // Here whe manually update schema for all configured datasources
        for (ProjectConfiguration project : configuration_.getProjects()) {
            updateDataSourceSchema(selectDataSource(project.getName()));
        }
    }

    /**
     *
     * Update datasource schema
     *
     * @param pDataSource
     * @since 1.0-SNAPSHOT
     */
    private void updateDataSourceSchema(DataSource pDataSource) {
        // 1. Update database schema if needed
        String dialect = configuration_.getDao().getDialect();
        if (configuration_.getDao().getEmbedded()) {
            dialect = DataSourcesConfiguration.EMBEDDED_HSQLDB_HIBERNATE_DIALECT;
        }
        MetadataSources metadata = new MetadataSources(new StandardServiceRegistryBuilder()
                .applySetting(Environment.DIALECT, dialect).applySetting(Environment.DATASOURCE, pDataSource).build());

        // 2 Add Entity for database mapping from classpath
        List<Class<?>> classes = DaoUtils.scanForJpaPackages(DaoUtils.PACKAGES_TO_SCAN, Entity.class,
                                                             InstanceEntity.class);
        classes.forEach(classe -> metadata.addAnnotatedClass(classe));

        SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true);
    }

    /**
     *
     * Add a datasource to the multitenant datasources pool
     *
     * @param pDataSource
     *            : Datasource to add
     * @param pTenant
     *            : Project name
     * @since 1.0-SNAPSHOT
     */
    private void addDataSource(DataSource pDataSource, String pTenant) {
        dataSources_.put(pTenant, pDataSource);
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
     * @param pTenant
     * @since 1.0-SNAPSHOT
     */
    public void addDataSource(String pUrl, String pUser, String pPassword, String pTenant) {

        if (configuration_.getDao().getEmbedded()) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(DataSourcesConfiguration.EMBEDDED_HSQL_DRIVER_CLASS);
            dataSource.setUrl(DataSourcesConfiguration.EMBEDDED_HSQL_URL + configuration_.getDao().getEmbeddedPath()
                    + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR + pTenant
                    + DataSourcesConfiguration.EMBEDDED_URL_SEPARATOR
                    + DataSourcesConfiguration.EMBEDDED_URL_BASE_NAME);

            addDataSource(dataSource, pTenant);
        }
        else {
            DataSourceBuilder factory = DataSourceBuilder.create()
                    .driverClassName(configuration_.getDao().getDriverClassName()).username(pUser).password(pPassword)
                    .url(pUrl);
            DataSource datasource = factory.build();
            addDataSource(datasource, pTenant);
        }
    }

}
