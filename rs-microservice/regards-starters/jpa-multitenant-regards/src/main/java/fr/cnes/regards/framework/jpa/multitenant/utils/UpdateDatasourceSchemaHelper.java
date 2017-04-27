/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 * Help to update datasource schema using hbm2ddl
 *
 * @author Marc Sordi
 *
 */
public class UpdateDatasourceSchemaHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDatasourceSchemaHelper.class);

    /**
     * Implicit naming strategy
     */
    private final String implicitNamingStrategyName;

    /**
     * Physical naming strategy
     */
    private final String physicalNamingStrategyName;

    /**
     * Microservice global configuration
     */
    private final MultitenantDaoProperties daoProperties;

    /**
     * Spring JPA Configuration
     */
    private final JpaProperties jpaProperties;

    /**
     * Common database properties
     */
    private Map<String, Object> dbProperties;

    public UpdateDatasourceSchemaHelper(final String implicitNamingStrategyName,
            final String physicalNamingStrategyName, final MultitenantDaoProperties daoProperties,
            final JpaProperties jpaProperties) {
        this.implicitNamingStrategyName = implicitNamingStrategyName;
        this.physicalNamingStrategyName = physicalNamingStrategyName;
        this.daoProperties = daoProperties;
        this.jpaProperties = jpaProperties;
    }

    /**
     *
     * Update datasource schema
     *
     * @param dataSource
     *            datasource to update
     */
    public void updateDataSourceSchema(final DataSource dataSource) throws JpaMultitenantException {

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();

        // Apply common settings
        builder.applySettings(getDbProperties(dataSource));
        // Specify data source
        builder.applySetting(Environment.DATASOURCE, dataSource);

        final MetadataSources metadata = new MetadataSources(builder.build());

        Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        packagesToScan.stream()
                .flatMap(pPackage -> DaoUtils.scanPackageForJpa(pPackage, Entity.class, InstanceEntity.class).stream())
                .forEach(metadata::addAnnotatedClass);

        final SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());
        export.execute(false, true);
    }

    /**
     * @return database properties
     * @throws JpaMultitenantException
     */
    public Map<String, Object> getDbProperties(final DataSource dataSource) throws JpaMultitenantException {
        if (dbProperties == null) {
            dbProperties = new HashMap<>();

            // Add Spring JPA hibernate properties
            // Schema must be retrieved here if managed with property :
            // spring.jpa.properties.hibernate.default_schema
            dbProperties.putAll(jpaProperties.getHibernateProperties(dataSource));
            // Remove hbm2ddl as schema update is done programmatically
            dbProperties.remove(Environment.HBM2DDL_AUTO);

            // Dialect
            String dialect = daoProperties.getDialect();
            if (daoProperties.getEmbedded()) {
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
                throw new JpaMultitenantException(e);
            }
        }
        return dbProperties;
    }
}
