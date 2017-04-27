/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.rest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mchange.v2.c3p0.PooledDataSource;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 *
 * Manage data source connection for Multitenant starter
 *
 * @author Marc Sordi
 *
 */
@RestController
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
@RequestMapping("/regards/{tenant}/datasource")
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
    @ResourceAccess(description = "Test the tenant connection", role = DefaultRole.PUBLIC)
    public ResponseEntity<Body> testTenantConnection(@PathVariable String tenant) {

        DataSource dataSource = dataSources.get(tenant);

        if (dataSource == null) {
            return ResponseEntity.badRequest().body(new Body("No datasource found for specified tenant"));
        }

        try (Connection connection = dataSource.getConnection()) {
            return ResponseEntity.ok(new Body("Valid connection"));
        } catch (SQLException e) {
            LOGGER.error("Connection test fail", e);
            return ResponseEntity.badRequest().body(new Body("Invalid connection for specified tenant"));
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/status")
    @ResourceAccess(description = "Get the tenant datasource status (for pooled one)", role = DefaultRole.PUBLIC)
    public ResponseEntity<Body> getDataSourceStatus(@PathVariable String tenant) {

        DataSource dataSource = dataSources.get(tenant);

        if (dataSource == null) {
            return ResponseEntity.badRequest().body(new Body("No datasource found for specified tenant"));
        }

        // Add datasource status if available
        if (dataSource instanceof PooledDataSource) {
            PooledDataSource pds = (PooledDataSource) dataSource;
            Body body = new Body();
            try {
                body.getProperties().put("num_connections", Integer.valueOf(pds.getNumConnectionsDefaultUser()));
                body.getProperties().put("num_busy_connections",
                                         Integer.valueOf(pds.getNumBusyConnectionsDefaultUser()));
                body.getProperties().put("num_idle_connections",
                                         Integer.valueOf(pds.getNumIdleConnectionsDefaultUser()));
                return ResponseEntity.ok(body);
            } catch (SQLException e) {
                LOGGER.error("Status check fail", e);
                return ResponseEntity.badRequest().body(new Body("Cannot retrieve status for specified tenant"));
            }

        } else {
            return ResponseEntity.ok(new Body("Status not available for unpooled datasource"));
        }
    }

    /**
     * HTTP body response message
     *
     * @author Marc Sordi
     *
     */
    private static class Body {

        private String message;

        private final Map<String, Object> properties = new HashMap<>();

        public Body() {
            // Default constructor
        }

        public Body(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setMessage(String pMessage) {
            message = pMessage;
        }
    }
}
