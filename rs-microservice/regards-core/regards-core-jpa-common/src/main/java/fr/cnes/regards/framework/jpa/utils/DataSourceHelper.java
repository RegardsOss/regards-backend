/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import javax.sql.DataSource;

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
public class DataSourceHelper {

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
     *
     * Create an embedded datasource
     *
     * @param pProjectName
     *            Project name
     * @param pEmbeddedPath
     *            path for database files.
     * @return DataSource
     * @since 1.0-SNAPSHOT
     */
    public static DataSource createEmbeddedDataSource(final String pProjectName, final String pEmbeddedPath) {

        final DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
        dmDataSource.setDriverClassName(EMBEDDED_HSQL_DRIVER_CLASS);
        dmDataSource.setUrl(EMBEDDED_HSQL_URL + pEmbeddedPath + DataSourceHelper.EMBEDDED_URL_SEPARATOR + pProjectName
                + DataSourceHelper.EMBEDDED_URL_SEPARATOR + DataSourceHelper.EMBEDDED_URL_BASE_NAME);
        return dmDataSource;
    }

    /**
     *
     * Create a datasource
     *
     * @param pUrl
     *            Adress to access database
     * @param pDriverClassName
     *            Driver class name
     * @param pUserName
     *            user name
     * @param pPassword
     *            password
     * @return DataSource
     * @since 1.0-SNAPSHOT
     */
    public static DataSource createDataSource(final String pUrl, final String pDriverClassName, final String pUserName,
            final String pPassword) {

        final DataSourceBuilder factory = DataSourceBuilder.create().driverClassName(pDriverClassName)
                .username(pUserName).password(pPassword).url(pUrl);
        return factory.build();
    }

}
