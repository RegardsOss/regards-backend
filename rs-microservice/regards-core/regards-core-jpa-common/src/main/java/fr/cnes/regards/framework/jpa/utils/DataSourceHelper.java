/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 *
 * Class DataSourceHelper
 *
 * Helper to manipulate JPA Regards Datasources
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public final class DataSourceHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceHelper.class);

    private static final String HR = "####################################################";

    /**
     * Hibernate dialect for embedded HSQL Database
     */
    public static final String EMBEDDED_HSQLDB_HIBERNATE_DIALECT = "org.hibernate.dialect.HSQLDialect";

    /**
     * Hibernate driver class for embedded HSQL Database
     */
    public static final String EMBEDDED_HSQL_DRIVER_CLASS = "org.hsqldb.jdbcDriver";

    /**
     * Url prefix for embedded HSQL Database. Persistence into file.
     */
    public static final String EMBEDDED_HSQL_URL = "jdbc:hsqldb:file:";

    /**
     * Data source URL separator
     */
    public static final String EMBEDDED_URL_SEPARATOR = "/";

    /**
     * HSQL Embedded Data source base name. Property shutdown allow to close the embedded database when the last
     * connection is close.
     */
    public static final String EMBEDDED_URL_BASE_NAME = "applicationdb;shutdown=true;";

    /**
     * Property to allow hibernate to select org.hibernate.id.enhanced.SequenceStyleGenerator instead of
     * org.hibernate.id.SequenceHiLoGenerator
     */
    public static final String HIBERNATE_ID_GENERATOR_PROP = "hibernate.id.new_generator_mappings";

    /**
     * Staticc class
     */
    private DataSourceHelper() {
    }

    /**
     *
     * Create an embedded datasource
     *
     * @param pTenant
     *            Project name
     * @param pEmbeddedPath
     *            path for database files.
     * @return DataSource
     * @since 1.0-SNAPSHOT
     */
    public static DataSource createEmbeddedDataSource(final String pTenant, final String pEmbeddedPath) {

        final DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
        dmDataSource.setDriverClassName(EMBEDDED_HSQL_DRIVER_CLASS);
        dmDataSource.setUrl(EMBEDDED_HSQL_URL + pEmbeddedPath + DataSourceHelper.EMBEDDED_URL_SEPARATOR + pTenant
                + DataSourceHelper.EMBEDDED_URL_SEPARATOR + DataSourceHelper.EMBEDDED_URL_BASE_NAME);

        LOGGER.info("\n{}\nCreating EMBEDDED datasource for tenant {} with path {}\n{}", HR, pTenant, pEmbeddedPath,
                    HR);

        return dmDataSource;
    }

    public static DataSource createDataSource(final String pTenant, final String pUrl, final String pDriverClassName,
            final String pUserName, final String pPassword) {

        final DataSourceBuilder factory = DataSourceBuilder.create().driverClassName(pDriverClassName)
                .username(pUserName).password(pPassword).url(pUrl);

        LOGGER.info("\n{}\nCreating datasource for tenant {} with url {}\n{}", HR, pTenant, pUrl, HR);

        return factory.build();
    }
}
