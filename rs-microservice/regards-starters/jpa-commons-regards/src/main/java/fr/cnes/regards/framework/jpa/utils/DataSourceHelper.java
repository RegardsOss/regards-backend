/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Class DataSourceHelper
 *
 * Helper to manipulate JPA Regards Datasources
 * @author CS
 */
public final class DataSourceHelper {

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
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceHelper.class);

    private static final String HR = "####################################################";

    /**
     * Static class
     */
    private DataSourceHelper() {
    }

    /**
     * Create an embedded data source. This method should not be used in production in favor of
     * {@link DataSourceHelper#createHikariDataSource(String, String, String, String, String, Integer, Integer, String, String)}
     * @param pTenant Project name
     * @param pEmbeddedPath path for database files.
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

    public static DataSource createHikariDataSource(String tenant, String url, String driverClassName, String userName,
            String password, Integer minPoolSize, Integer maxPoolSize, String preferredTestQuery, String schemaIdentifier) throws IOException {

        LOGGER.info("\n{}\nCreating a HIKARI CP datasource for tenant {} with url {}\n{}", HR, tenant, url, HR);

        try (InputStream hikariPropertiesIS = DataSourceHelper.class.getResourceAsStream("hikari.properties")) {
            // Loading static properties
            Properties properties = new Properties();
            properties.load(hikariPropertiesIS);

            HikariConfig config = new HikariConfig(properties);
            config.setJdbcUrl(url);
            config.setUsername(userName);
            config.setPassword(password);
            // For maximum performance, HikariCP does not recommend setting this value so minimumIdle = maximumPoolSize
            config.setMinimumIdle(minPoolSize);
            config.setMaximumPoolSize(maxPoolSize);
            config.setPoolName(String.format("Hikari-Pool-%s", tenant));
            config.setIdleTimeout(30000L);
            // Docker Swarm / Kube workaround: https://github.com/brettwooldridge/HikariCP/issues/1237
            // maxLifetime should be 10mins to avoid stale postgres connections
            config.setMaxLifetime(600000L);
            // Postgres schema configuration
            config.setConnectionInitSql("SET search_path to " + schemaIdentifier);

            return new HikariDataSource(config);
        }
    }

    /**
     * Test connection
     * @param dataSource data source to test
     * @param destroyOnFail if true, destroy datasource if connection test fails.
     * @throws SQLException if connection fails
     */
    public static void testConnection(DataSource dataSource, boolean destroyOnFail) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            LOGGER.debug("Successful data source connection test");
        } catch (SQLException e) {
            LOGGER.error("Data source connection fails.", e);
            if (destroyOnFail) {
                LOGGER.error("Destroying data source", dataSource.toString());
                dataSource.unwrap(HikariDataSource.class).close();
            }
            throw e;
        }
    }
}
