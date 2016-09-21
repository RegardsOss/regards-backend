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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import fr.cnes.regards.microservices.core.dao.jpa.MultitenancyProperties;

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

    static final Logger LOG = LoggerFactory.getLogger(DataSourceBasedMultiTenantConnectionProviderImpl.class);

    private Map<String, DataSource> dataSources;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MultitenancyProperties multitenancyProperties;

    @PostConstruct
    public void load() {
        dataSources = new HashMap<>();
        // Adding default condigured datasurce
        dataSources.put(multitenancyProperties.getTenant(), dataSource);
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSources.get(multitenancyProperties.getTenant());
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return dataSources.get(tenantIdentifier);
    }

    public void addDataSource(DataSource pDataSource, String pTenant, String pHibernateDialiect) {

        // 1. Add datasource to datasource pool map
        dataSources.put(pTenant, pDataSource);

        // 2. Update database schema if needed
        MetadataSources metadata = new MetadataSources(
                new StandardServiceRegistryBuilder().applySetting("hibernate.dialect", pHibernateDialiect)
                        .applySetting("hibernate.connection.datasource", pDataSource).build());

        // 2.1 Add Entity for database mapping from classpath
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (BeanDefinition def : scanner.findCandidateComponents(PACKAGE_TO_SCAN)) {
            try {
                metadata.addAnnotatedClass(Class.forName(def.getBeanClassName()));
            }
            catch (ClassNotFoundException e) {
                LOG.error("Error adding entity to scan : " + def.getBeanClassName());
            }
        }

        SchemaExport export = new SchemaExport((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true, false, false);

    }

    public void addDataSource(String pUrl, String pUser, String pPassword, String pTenant, String pHibernateDialect) {

        DataSourceBuilder factory = DataSourceBuilder.create(multitenancyProperties.getDatasource().getClassLoader())
                .driverClassName(multitenancyProperties.getDatasource().getDriverClassName()).username(pUser)
                .password(pPassword).url(pUrl);
        DataSource datasource = factory.build();

        addDataSource(datasource, pTenant, pHibernateDialect);
    }

}
