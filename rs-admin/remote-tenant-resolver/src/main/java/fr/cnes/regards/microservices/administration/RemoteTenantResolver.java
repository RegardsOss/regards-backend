/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

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
     * Initial Feign client to administration service to retrieve informations about projects
     */
    private final IProjectsClient projectsClient;

    /**
     * Feign security manager
     */
    private final FeignSecurityManager feignSecurityManager;

    /**
     * Eureka discovery client
     */
    private final DiscoveryClient discoveryClient;

    public RemoteTenantResolver(String pAdminMicroserviceName, DiscoveryClient pDiscoveryClient,
            FeignSecurityManager pFeignSecurityManager) {
        discoveryClient = pDiscoveryClient;
        this.feignSecurityManager = pFeignSecurityManager;

        final List<ServiceInstance> instances = discoveryClient.getInstances(pAdminMicroserviceName);
        if (instances.isEmpty()) {
            String errorMessage = "No administration instance found. Microservice cannot start.";
            LOGGER.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        // Init project client programmatically
        projectsClient = FeignClientBuilder.build(new TokenClientProvider<>(IProjectsClient.class,
                instances.get(0).getUri().toString(), feignSecurityManager));
    }

    @Override
    public Set<String> getAllTenants() {
        Set<String> tenants = new HashSet<>();
        try {
            // Bypass authorization for internal request
            FeignSecurityManager.asSystem();

            final List<Resource<Project>> resources = new ArrayList<>();

            boolean nextProjectsPage;
            int projectPage = 0;
            // Get all tenants with projects clients pageable requests
            do {
                final ResponseEntity<PagedResources<Resource<Project>>> response = projectsClient
                        .retrieveProjectList(projectPage, 100);
                if ((response != null) && response.getStatusCode().equals(HttpStatus.OK)
                        && (response.getBody() != null)) {
                    nextProjectsPage = projectPage++ == response.getBody().getMetadata().getTotalPages();
                    resources.addAll(response.getBody().getContent());
                } else {
                    LOGGER.error("Error during remote request to administration service");
                    nextProjectsPage = false;
                }
            } while (!nextProjectsPage);

            final List<Project> projects = HateoasUtils.unwrapList(resources);
            projects.forEach(p -> tenants.add(p.getName()));

            return tenants;
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
