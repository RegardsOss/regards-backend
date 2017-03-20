/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
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
public class RemoteTenantResolver implements ITenantResolver {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteTenantResolver.class);

    /**
     * Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Initial Feign client to administration service to retrieve informations about projects
     */
    private ITenantClient tenantClient;

    /**
     * Feign security manager
     */
    private final FeignSecurityManager feignSecurityManager;

    /**
     * Eureka discovery client
     */
    private final DiscoveryClient discoveryClient;

    public RemoteTenantResolver(DiscoveryClient pDiscoveryClient, FeignSecurityManager pFeignSecurityManager) {
        discoveryClient = pDiscoveryClient;
        this.feignSecurityManager = pFeignSecurityManager;
    }

    @PostConstruct
    public void initClient() {
        final List<ServiceInstance> instances = discoveryClient.getInstances("rs-admin");
        if (instances.isEmpty()) {
            String errorMessage = "No administration instance found. Microservice cannot start.";
            LOGGER.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        // Init project client programmatically
        tenantClient = FeignClientBuilder.build(new TokenClientProvider<>(ITenantClient.class,
                instances.get(0).getUri().toString(), feignSecurityManager));
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
