/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.rest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mchange.v2.c3p0.PooledDataSource;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.module.rest.representation.GenericResponseBody;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Manage data source connection for Multitenant starter
 * @author Marc Sordi
 */
@RestController
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
@RequestMapping(value = "/regards/{tenant}/datasource", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class JpaMultitenantController {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaMultitenantController.class);

    /**
     * Data source registry
     */
    @Autowired
    @Qualifier(DataSourcesAutoConfiguration.DATA_SOURCE_BEAN_NAME)
    private Map<String, DataSource> dataSources;

    @RequestMapping(method = RequestMethod.GET, value = "/test")
    @ResourceAccess(description = "Test the tenant connection", role = DefaultRole.ADMIN)
    public ResponseEntity<GenericResponseBody> testTenantConnection(@PathVariable String tenant) {

        DataSource dataSource = dataSources.get(tenant);

        if (dataSource == null) {
            return ResponseEntity.badRequest()
                    .body(new GenericResponseBody("No datasource found for specified tenant"));
        }

        try (Connection connection = dataSource.getConnection()) {
            return ResponseEntity.ok(new GenericResponseBody("Valid connection"));
        } catch (SQLException e) {
            LOGGER.error("Connection test fail", e);
            return ResponseEntity.badRequest().body(new GenericResponseBody("Invalid connection for specified tenant"));
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/status")
    @ResourceAccess(description = "Get the tenant datasource status (for pooled one)", role = DefaultRole.PUBLIC)
    public ResponseEntity<GenericResponseBody> getDataSourceStatus(@PathVariable String tenant) {

        DataSource dataSource = dataSources.get(tenant);

        if (dataSource == null) {
            return ResponseEntity.badRequest()
                    .body(new GenericResponseBody("No datasource found for specified tenant"));
        }

        // Add datasource status if available
        if (dataSource instanceof HikariDataSource) {
            @SuppressWarnings("resource") // Data source is not close here!
            HikariDataSource hds = (HikariDataSource) dataSource;
            HikariPoolMXBean bean = hds.getHikariPoolMXBean();
            GenericResponseBody body = new GenericResponseBody();
            body.getProperties().put("Max connections", hds.getMaximumPoolSize());
            body.getProperties().put("Min idle connections", hds.getMinimumIdle());
            body.getProperties().put("Idle timeout", hds.getIdleTimeout());
            // Live info
            body.getProperties().put("Total connections", bean.getTotalConnections());
            body.getProperties().put("Idle connections", bean.getIdleConnections());
            body.getProperties().put("Threads awaiting connection", bean.getThreadsAwaitingConnection());
            body.getProperties().put("Active connection", bean.getActiveConnections());
            return ResponseEntity.ok(body);
        } else if (dataSource instanceof PooledDataSource) {
            PooledDataSource pds = (PooledDataSource) dataSource;
            GenericResponseBody body = new GenericResponseBody();
            try {
                body.getProperties().put("num_connections", pds.getNumConnectionsDefaultUser());
                body.getProperties().put("num_busy_connections", pds.getNumBusyConnectionsDefaultUser());
                body.getProperties().put("num_idle_connections", pds.getNumIdleConnectionsDefaultUser());
                return ResponseEntity.ok(body);
            } catch (SQLException e) {
                LOGGER.error("Status check fail", e);
                return ResponseEntity.badRequest()
                        .body(new GenericResponseBody("Cannot retrieve status for specified tenant"));
            }
        } else {
            return ResponseEntity.ok(new GenericResponseBody("Status not available for unpooled datasource"));
        }
    }
}
