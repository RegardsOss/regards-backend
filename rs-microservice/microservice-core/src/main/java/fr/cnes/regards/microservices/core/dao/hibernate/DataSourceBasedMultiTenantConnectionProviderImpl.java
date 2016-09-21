/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.hibernate;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.configuration.common.ProjectConfiguration;

/**
 *
 * Multitenancy Database Connection Provider. By default only one connection is available. The one defined in the
 * DataSourceConfig class. To add a new connection use the addDataSource method
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final long serialVersionUID = 8168907057647334460L;

    private static final String PACKAGE_TO_SCAN = "fr.cnes.regards";

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceBasedMultiTenantConnectionProviderImpl.class);

    @Autowired
    private MicroserviceConfiguration configuration_;

    /**
     * Pool of datasources available for this connection provider
     */
    @Autowired
    @Qualifier("dataSources")
    private final Map<String, DataSource> dataSources_ = new HashMap<>();

    @Override
    protected DataSource selectAnyDataSource() {
        DataSource datasource = dataSources_.get(configuration_.getProjects().get(0).getName());
        return datasource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        DataSource datasource = dataSources_.get(tenantIdentifier);
        return datasource;
    }

    @PostConstruct
    public void initDataSources() {

        // Hibernate do not initialize schema for multitenants.
        // Here whe manually update schema for all datasources

        for (ProjectConfiguration project : configuration_.getProjects()) {
            updateDataSourceSchema(selectDataSource(project.getName()));
        }

    }

    private void updateDataSourceSchema(DataSource pDataSource) {
        // 1. Update database schema if needed
        MetadataSources metadata = new MetadataSources(new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", configuration_.getDao().getDialect())
                .applySetting("hibernate.connection.datasource", pDataSource).build());

        // 2 Add Entity for database mapping from classpath
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (BeanDefinition def : scanner.findCandidateComponents(PACKAGE_TO_SCAN)) {
            try {
                metadata.addAnnotatedClass(Class.forName(def.getBeanClassName()));
            }
            catch (ClassNotFoundException e) {
                LOG.error("Error adding entity " + def.getBeanClassName() + " for hibernate database update");
                LOG.error(e.getMessage(), e);
            }
        }

        SchemaExport export = new SchemaExport((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true, false, false);
    }

    public void addDataSource(DataSource pDataSource, String pTenant, String pHibernateDialiect) {
        dataSources_.put(pTenant, pDataSource);
        updateDataSourceSchema(pDataSource);
    }

    public void addDataSource(String pUrl, String pUser, String pPassword, String pTenant, String pHibernateDialect) {

        // multitenancyProperties_.getDatasource().getClassLoader()
        DataSourceBuilder factory = DataSourceBuilder.create()
                .driverClassName(configuration_.getDao().getDriverClassName()).username(pUser).password(pPassword)
                .url(pUrl);
        DataSource datasource = factory.build();

        addDataSource(datasource, pTenant, pHibernateDialect);
    }

}
