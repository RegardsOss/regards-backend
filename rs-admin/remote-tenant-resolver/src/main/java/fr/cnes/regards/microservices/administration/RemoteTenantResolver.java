/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.Set;

import org.springframework.cloud.client.discovery.DiscoveryClient;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.client.rest.ITenantClient;

/**
 *
 * Class RemoteTenantResolver
 *
 * Microservice remote tenant resolver. Retrieve tenants from the administration microservice.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class RemoteTenantResolver extends AbstractDiscoveryClientChecker implements ITenantResolver {

    /**
     * Microservice name
     */
    private final String microserviceName;

    /**
     * Initial Feign client to administration service to retrieve informations about projects
     */
    private final ITenantClient tenantClient;

    public RemoteTenantResolver(final DiscoveryClient pDiscoveryClient, ITenantClient tenantClient,
            String microserviceName) {
        super(pDiscoveryClient);
        this.microserviceName = microserviceName;
        this.tenantClient = tenantClient;
    }

    @Override
    public Set<String> getAllTenants() {
        try {
            // Bypass authorization for internal request
            FeignSecurityManager.asSystem();
            return tenantClient.getAllTenants().getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public Set<String> getAllActiveTenants() {
        try {
            // Bypass authorization for internal request
            FeignSecurityManager.asSystem();
            return tenantClient.getAllActiveTenants(microserviceName).getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
