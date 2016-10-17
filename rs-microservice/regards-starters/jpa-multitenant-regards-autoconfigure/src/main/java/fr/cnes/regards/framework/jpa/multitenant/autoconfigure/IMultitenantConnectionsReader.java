/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.List;

import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class IMultitenantConnectionsReader
 *
 * Interface to create a custom datasources configuration reader. All datasources returned by the method getDataSources
 * are managed by regards multitenancy jpa.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IMultitenantConnectionsReader {

    /**
     *
     * Retrieve the connection configuration for each tenant.
     *
     * @return
     * @since 1.0-SNAPSHOT
     */
    public List<ProjectConnection> getTenantConnections();
}
