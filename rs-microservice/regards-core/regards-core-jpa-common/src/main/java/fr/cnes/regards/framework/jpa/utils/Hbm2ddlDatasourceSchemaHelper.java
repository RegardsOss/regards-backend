/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.utils;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Help to update datasource schema using hbm2ddl
 *
 * @author Marc Sordi
 *
 */
public class Hbm2ddlDatasourceSchemaHelper extends AbstractDataSourceSchemaHelper {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Hbm2ddlDatasourceSchemaHelper.class);

    /**
     * If set, allows to export initialization script
     */
    private String outputFile = null;

    /**
     * Entity annotation to manage
     */
    private final Class<? extends Annotation> includeAnnotation;

    /**
     * Entity annotation to skip
     */
    private final Class<? extends Annotation> excludeAnnotation;

    public Hbm2ddlDatasourceSchemaHelper(Map<String, Object> hibernateProperties,
            Class<? extends Annotation> includeAnnotation, Class<? extends Annotation> excludeAnnotation) {
        super(hibernateProperties);
        this.includeAnnotation = includeAnnotation;
        this.excludeAnnotation = excludeAnnotation;
    }

    /**
     *
     * Update datasource schema
     *
     * @param dataSource datasource to update
     * @param rootPackageToScan package to scan for JPA entities
     * @param schema forced target database schema removing existing (if any) from properties
     */
    public void migrate(final DataSource dataSource, String rootPackageToScan, String schema) {

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
        packagesToScan.stream().flatMap(pPackage -> DaoUtils
                .scanPackageForJpa(pPackage, includeAnnotation, excludeAnnotation).stream())
                .forEach(metadata::addAnnotatedClass);

        final SchemaUpdate export = new SchemaUpdate((MetadataImplementor) metadata.buildMetadata());

        if (outputFile != null) {
            export.setOutputFile(outputFile);
            export.setDelimiter(";");
            export.execute(true, true);
        } else {
            export.execute(false, true);
        }

    }

    public void migrate(final DataSource dataSource, String rootPackageToScan) {
        migrate(dataSource, rootPackageToScan, null);
    }

    @Override
    public void migrate(final DataSource dataSource) {
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
}
