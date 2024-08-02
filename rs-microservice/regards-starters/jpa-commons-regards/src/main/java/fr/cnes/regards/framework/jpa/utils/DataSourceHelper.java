/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Class DataSourceHelper
 * Helper to manipulate JPA Regards Datasources
 *
 * @author CS
 */
public final class DataSourceHelper {

    /**
     * Hibernate dialect for embedded H2 Database
     */
    public static final String EMBEDDED_H2_HIBERNATE_DIALECT = "org.hibernate.dialect.H2Dialect";

    /**
     * Hibernate driver class for embedded H2 Database
     */
    public static final String EMBEDDED_H2_DRIVER_CLASS = "org.h2.Driver";

    /**
     * Url prefix for embedded H2 Database. Persistence into file.
     */
    public static final String EMBEDDED_H2_URL = "jdbc:h2:file:./";

    /**
     * Data source URL separator
     */
    public static final String EMBEDDED_URL_SEPARATOR = "/";

    /**
     * H2 Embedded Data source base name.
     */
    public static final String EMBEDDED_URL_BASE_NAME = "applicationdb;";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceHelper.class);

    private static final String HR = "####################################################";

    private static final long CONNECTION_MAX_LIFE_TIME_MS = MINUTES.toMillis(10);

    private static final long CONNECTION_TIMEOUT_MS = MINUTES.toMillis(9);

    private static final long CONNECTION_IDLE_TIMEOUT_MS = MINUTES.toMillis(8);

    /**
     * Static class
     */
    private DataSourceHelper() {
    }

    /**
     * Create an embedded data source. This method should not be used in production in favor of
     * {@link DataSourceHelper#createHikariDataSource(String, String, String, String, String, Integer, Integer, String, String)}
     *
     * @param tenant       Project name
     * @param embeddedPath path for database files.
     * @return an HSQLDB embedded data source
     */
    public static DataSource createEmbeddedDataSource(String tenant, String embeddedPath) {

        final DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
        dmDataSource.setDriverClassName(EMBEDDED_H2_DRIVER_CLASS);
        dmDataSource.setUrl(EMBEDDED_H2_URL
                            + embeddedPath
                            + DataSourceHelper.EMBEDDED_URL_SEPARATOR
                            + tenant
                            + DataSourceHelper.EMBEDDED_URL_SEPARATOR
                            + DataSourceHelper.EMBEDDED_URL_BASE_NAME);

        LOGGER.info("\n{}\nCreating an EMBEDDED datasource for tenant {} with path {}\n{}",
                    HR,
                    tenant,
                    embeddedPath,
                    HR);

        return dmDataSource;
    }

    public static DataSource createHikariDataSource(String tenant,
                                                    String url,
                                                    String driverClassName,
                                                    String userName,
                                                    String password,
                                                    Integer minPoolSize,
                                                    Integer maxPoolSize,
                                                    String preferredTestQuery,
                                                    String schemaIdentifier,
                                                    long connectionAcquisitionThresholdLoggerLimitMs)
        throws IOException {

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
            // Maximum time in milliseconds that HikariCP will wait for a connection from the pool before throwing an
            // exception
            // Default value is 30seconds (-5 for validation) see HikariConfig#CONNECTION_TIMEOUT_MS
            config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
            // maximum amount of time that a connection can sit idle in the pool before being closed
            // Default value is 10 minutes see HikariConfig#CONNECTION_IDLE_TIMEOUT_MS
            config.setIdleTimeout(CONNECTION_IDLE_TIMEOUT_MS);
            // Maximum lifetime of a connection in the pool, whether it is in use or not.
            // This helps to periodically renew connections to avoid issues with connections becoming stale or corrupted over time.
            // Docker Swarm / Kube workaround: https://github.com/brettwooldridge/HikariCP/issues/1237
            // maxLifetime should be 10mins to avoid stale postgres connections
            config.setMaxLifetime(CONNECTION_MAX_LIFE_TIME_MS);
            // Postgres schema configuration
            config.setConnectionInitSql("SET search_path to " + schemaIdentifier);

            return new HikariDataSourceCustom(config, connectionAcquisitionThresholdLoggerLimitMs);
        }
    }

    /**
     * Test connection
     *
     * @param dataSource    data source to test
     * @param destroyOnFail if true, destroy datasource if connection test fails.
     * @throws SQLException if connection fails
     */
    public static void testConnection(DataSource dataSource, boolean destroyOnFail) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            LOGGER.debug("Successful data source connection test");
        } catch (SQLException e) {
            LOGGER.error("Data source connection fails.", e);
            if (destroyOnFail) {
                LOGGER.error("Destroying data source", dataSource);
                dataSource.unwrap(HikariDataSource.class).close();
            }
            throw e;
        }
    }
}
