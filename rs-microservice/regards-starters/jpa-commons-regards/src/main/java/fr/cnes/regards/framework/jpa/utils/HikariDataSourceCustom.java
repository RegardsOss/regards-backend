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
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Overrides {@link HikariDataSource} to log connections information.
 *
 * @author Sébastien Binda
 **/
public class HikariDataSourceCustom extends HikariDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HikariDataSourceCustom.class);

    private final long connectionAcquisitionThresholdLoggerLimitMs;

    public HikariDataSourceCustom(HikariConfig config, long connectionAcquisitionThresholdLoggerLimitMs) {
        super(config);
        this.connectionAcquisitionThresholdLoggerLimitMs = connectionAcquisitionThresholdLoggerLimitMs;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            long start = System.currentTimeMillis();
            Connection connection = super.getConnection();
            long duration = System.currentTimeMillis() - start;
            if (duration > this.connectionAcquisitionThresholdLoggerLimitMs) {
                LOGGER.warn(String.format("Hikari pool connection acquired in %d ms.", duration));
            }
            return connection;
        } catch (SQLException exception) {
            logDataSourceCurrentInformation();
            throw exception;
        }
    }

    private void logDataSourceCurrentInformation() {
        LOGGER.error("#############################################");
        LOGGER.error(" - Hikari pool running : {}", this.isRunning());
        LOGGER.error(" - Max connections {}", this.getMaximumPoolSize());
        LOGGER.error(" - Connection timeout {}", this.getConnectionTimeout());
        LOGGER.error(" - Idle timeout {}", this.getIdleTimeout());
        // Live info
        HikariPoolMXBean pool = this.getHikariPoolMXBean();
        if (pool != null) {
            LOGGER.error(" - Current pool information:");
            LOGGER.error("   + Total connections {}", pool.getTotalConnections());
            LOGGER.error("   + Idle connections {}", pool.getIdleConnections());
            LOGGER.error("   + Threads awaiting connection {}", pool.getThreadsAwaitingConnection());
            LOGGER.error("   + Active connections {}", pool.getActiveConnections());
        }
        LOGGER.error("#############################################");
    }
}
