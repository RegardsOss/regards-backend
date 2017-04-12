/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.List;

import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
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
     * Retrieve the connection configuration for each tenant of the specified microservice
     *
     * @param microserviceName
     *            related microservice
     * @return List of existing {@link TenantConnection}
     * @since 1.0-SNAPSHOT
     */
    List<TenantConnection> getTenantConnections(String microserviceName) throws JpaMultitenantException;

    /**
     *
     * Add a new tenant connection
     *
     * @param microserviceName
     *            related microservice
     * @param pTenantConnection
     *            tenant connection for specified microservice
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void addTenantConnection(String microserviceName, TenantConnection tenantConnection) throws JpaMultitenantException;

    /**
     * Enable a tenant connection
     *
     * @param microserviceName
     *            related microservice
     * @param tenant
     *            related tenant
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void enableTenantConnection(String microserviceName, String tenant) throws JpaMultitenantException;

    /**
     * Disable a tenant connection
     *
     * @param microserviceName
     *            related microservice
     * @param tenant
     *            related tenant
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void disableTenantConnection(String microserviceName, String tenant) throws JpaMultitenantException;
}
