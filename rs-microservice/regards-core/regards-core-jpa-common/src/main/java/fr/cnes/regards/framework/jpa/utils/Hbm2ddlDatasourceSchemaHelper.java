/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.JpaException;

/**
 * Help to update datasource schema using hbm2ddl
 *
 * @author Marc Sordi
 *
 */
public class Hbm2ddlDatasourceSchemaHelper implements IDatasourceSchemaHelper {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Hbm2ddlDatasourceSchemaHelper.class);

    /**
     * Hibernate properties that may impact migration configuration
     */
    private final Map<String, Object> hibernateProperties;

    /**
     * If set, allows to export initialization script
     */
    private String outputFile = null;

    public Hbm2ddlDatasourceSchemaHelper(Map<String, Object> hibernateProperties) {
        this.hibernateProperties = hibernateProperties;
    }

    /**
     *
     * Update datasource schema
     *
     * @param dataSource datasource to update
     * @param rootPackageToScan package to scan for JPA entities
     * @param schema forced target database schema removing existing (if any) from properties
     * @throws JpaException if error occurs!
     */
    public void migrate(final DataSource dataSource, String rootPackageToScan, String schema) throws JpaException {

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();

        // Apply common settings
        builder.applySettings(hibernateProperties);
        // Forced schema if any (may override property one)
        if (schema != null) {
            builder.applySetting(Environment.DEFAULT_SCHEMA, schema);
        }
        // Specify data source
        builder.applySetting(Environment.DATASOURCE, dataSource);

        final MetadataSources metadata = new MetadataSources(builder.build());

        Set<String> packagesToScan = DaoUtils.findPackagesForJpa(rootPackageToScan);
        packagesToScan.stream()
                .flatMap(pPackage -> DaoUtils.scanPackageForJpa(pPackage, Entity.class, InstanceEntity.class).stream())
                .forEach(metadata::addAnnotatedClass);

        final SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());

        if (outputFile != null) {
            export.setOutputFile(outputFile);
            export.execute(true, true);
        } else {
            export.execute(false, true);
        }

    }

    public void migrate(final DataSource dataSource, String rootPackageToScan) throws JpaException {
        migrate(dataSource, rootPackageToScan, null);
    }

    @Override
    public void migrate(final DataSource dataSource) throws JpaException {
        migrate(dataSource, DaoUtils.ROOT_PACKAGE, null);
    }

    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Set an output file to export SQL script. Default : none.
     * @param pOutputFile SQL script output file
     */
    public void setOutputFile(String pOutputFile) {
        outputFile = pOutputFile;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper#getHibernateProperties()
     */
    @Override
    public Map<String, Object> getHibernateProperties() {
        return hibernateProperties;
    }
}
