/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

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
     * Static class
     */
    private DataSourceHelper() {
    }

    /**
     *
     * Create an embedded data source. This method should not be used in production in favor of
     * {@link DataSourceHelper#createPooledDataSource(String, String, String, String, String, Integer, Integer)}
     *
     * @param pTenant
     *            Project name
     * @param pEmbeddedPath
     *            path for database files.
     * @return an HSQLDB embedded data source
     */
    public static DataSource createEmbeddedDataSource(final String pTenant, final String pEmbeddedPath) {

        final DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
        dmDataSource.setDriverClassName(EMBEDDED_HSQL_DRIVER_CLASS);
        dmDataSource.setUrl(EMBEDDED_HSQL_URL + pEmbeddedPath + DataSourceHelper.EMBEDDED_URL_SEPARATOR + pTenant
                + DataSourceHelper.EMBEDDED_URL_SEPARATOR + DataSourceHelper.EMBEDDED_URL_BASE_NAME);

        LOGGER.info("\n{}\nCreating an EMBEDDED datasource for tenant {} with path {}\n{}", HR, pTenant, pEmbeddedPath,
                    HR);

        return dmDataSource;
    }

    /**
     * Create an unpooled {@link DataSource}. This method should not be used in production in favor of
     * {@link DataSourceHelper#createPooledDataSource(String, String, String, String, String, Integer, Integer)}
     *
     * @param pTenant
     *            related tenant, only useful for login purpose
     * @param pUrl
     *            data source URL
     * @param pDriverClassName
     *            data source driver
     * @param pUserName
     *            the user to used for the database connection
     * @param pPassword
     *            the user's password to used for the database connection
     * @return an unpooled {@link DataSource}
     */
    public static DataSource createUnpooledDataSource(final String pTenant, final String pUrl,
            final String pDriverClassName, final String pUserName, final String pPassword) {

        final DataSourceBuilder factory = DataSourceBuilder.create().driverClassName(pDriverClassName)
                .username(pUserName).password(pPassword).url(pUrl);

        LOGGER.info("\n{}\nCreating an UNPOOLED datasource for tenant {} with url {}\n{}", HR, pTenant, pUrl, HR);

        return factory.build();
    }

    /**
     * Create a pooled {@link DataSource} using {@link ComboPooledDataSource}.
     *
     * @param pTenant
     *            related tenant, only useful for login purpose
     * @param pUrl
     *            data source URL
     * @param pDriverClassName
     *            data source driver
     * @param pUserName
     *            the user to used for the database connection
     * @param pPassword
     *            the user's password to used for the database connection
     * @param pMinPoolSize
     *            Minimum number of Connections a pool will maintain at any given time.
     * @param pMaxPoolSize
     *            Maximum number of Connections a pool will maintain at any given time.
     * @param pPreferredTestQuery
     *            preferred test query
     * @throws PropertyVetoException
     *             See {@link PropertyVetoException}
     */
    public static DataSource createPooledDataSource(final String pTenant, final String pUrl,
            final String pDriverClassName, final String pUserName, final String pPassword, Integer pMinPoolSize,
            Integer pMaxPoolSize, String pPreferredTestQuery) throws PropertyVetoException {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        cpds.setJdbcUrl(pUrl);
        cpds.setUser(pUserName);
        cpds.setPassword(pPassword);
        cpds.setMinPoolSize(pMinPoolSize);
        cpds.setMaxPoolSize(pMaxPoolSize);
        cpds.setDriverClass(pDriverClassName);
        cpds.setPreferredTestQuery(pPreferredTestQuery);

        LOGGER.info("\n{}\nCreating a POOLED datasource for tenant {} with url {}\n{}", HR, pTenant, pUrl, HR);

        return cpds;
    }
}
