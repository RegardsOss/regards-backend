/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.List;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.modules.project.client.rest.ITenantConnectionClient;

/**
 *
 * Class DefaultMultitenantConnectionsReader
 *
 * Default tenants connections configuration reader. Reads tenants from the microservice "rs-admin". Enabled, only if
 * the microservice is Eureka client.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceTenantConnectionResolver implements ITenantConnectionResolver {

    /**
     * Tenant connection client
     */
    private final ITenantConnectionClient tenantConnectionClient;

    public MicroserviceTenantConnectionResolver(ITenantConnectionClient tenantConnectionClient) {
        super();
        this.tenantConnectionClient = tenantConnectionClient;
    }

    @Override
    public List<TenantConnection> getTenantConnections(String microserviceName) throws JpaMultitenantException {
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<List<TenantConnection>> response = tenantConnectionClient
                    .getTenantConnections(microserviceName);
            return response.getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void addTenantConnection(String microserviceName, final TenantConnection pTenantConnection)
            throws JpaMultitenantException {
        try {
            FeignSecurityManager.asSystem();
            tenantConnectionClient.addTenantConnection(microserviceName, pTenantConnection);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void enableTenantConnection(String pMicroserviceName, String pTenant) throws JpaMultitenantException {
        try {
            FeignSecurityManager.asSystem();
            tenantConnectionClient.enableTenantConnection(pMicroserviceName, pTenant);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void disableTenantConnection(String pMicroserviceName, String pTenant) throws JpaMultitenantException {
        try {
            FeignSecurityManager.asSystem();
            tenantConnectionClient.disableTenantConnection(pMicroserviceName, pTenant);
        } finally {
            FeignSecurityManager.reset();
        }

    }

}
