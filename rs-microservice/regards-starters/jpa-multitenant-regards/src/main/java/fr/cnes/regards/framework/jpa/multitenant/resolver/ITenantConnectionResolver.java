/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.List;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 * Class IMultitenantConnectionsReader
 *
 * Interface to create a custom datasources configuration reader. All datasources returned by the method getDataSources
 * are managed by regards multitenancy jpa.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface ITenantConnectionResolver {

    /**
     *
     * Retrieve the connection configuration for each tenant.
     *
     * @return List of existing {@link TenantConnection}
     * @since 1.0-SNAPSHOT
     */
    List<TenantConnection> getTenantConnections();

    /**
     *
     * Add a new tenant connection
     *
     * @param pTenantConnection
     * @since 1.0-SNAPSHOT
     */
    void addTenantConnection(TenantConnection pTenantConnection);
}
