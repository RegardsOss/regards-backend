/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.List;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
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
     * Current Microservice name
     */
    private final String microserviceName;

    /**
     * Tenant connection client
     */
    private final ITenantConnectionClient tenantConnectionClient;

    public MicroserviceTenantConnectionResolver(final String microserviceName,
            ITenantConnectionClient tenantConnectionClient) {
        super();
        this.microserviceName = microserviceName;
        this.tenantConnectionClient = tenantConnectionClient;
    }

    @Override
    public List<TenantConnection> getTenantConnections() {
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
    public void addTenantConnection(final TenantConnection pTenantConnection) {
        try {
            FeignSecurityManager.asSystem();
            tenantConnectionClient.addTenantConnection(microserviceName, pTenantConnection);
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
