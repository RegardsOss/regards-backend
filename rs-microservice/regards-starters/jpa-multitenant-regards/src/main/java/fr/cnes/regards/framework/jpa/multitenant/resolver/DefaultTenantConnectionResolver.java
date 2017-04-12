/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 * Class DefaultTenantConnectionResolver
 *
 * Default resolver. Return empty list
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class DefaultTenantConnectionResolver implements ITenantConnectionResolver {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTenantConnectionResolver.class);

    @Override
    public List<TenantConnection> getTenantConnections(String microserviceName) throws JpaMultitenantException {
        LOGGER.warn("No Tenant connections resolver defined. Default one used.");
        return new ArrayList<>();
    }

    @Override
    public void addTenantConnection(String microserviceName, final TenantConnection pTenantConnection)
            throws JpaMultitenantException {
        LOGGER.warn("No Tenant connections resolver defined. Tenant connection is not persisted.");
    }

    @Override
    public void enableTenantConnection(String pMicroserviceName, String pTenant) throws JpaMultitenantException {
        // Nothing to do
    }

    @Override
    public void disableTenantConnection(String pMicroserviceName, String pTenant) throws JpaMultitenantException {
        // Nothing to do
    }

}
