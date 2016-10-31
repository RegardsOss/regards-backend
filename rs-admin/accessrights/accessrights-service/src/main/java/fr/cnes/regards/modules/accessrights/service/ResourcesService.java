/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import feign.FeignException;
import fr.cnes.regards.framework.security.client.IResourcesClient;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.utils.client.TokenClientProvider;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 * Class ResourceService
 *
 * Business service for Resources entities
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Service
public class ResourcesService implements IResourcesService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesService.class);

    /**
     * Eureka discovery client to access other microservices.
     */
    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * JPA Repository
     */
    @Autowired
    private IResourcesAccessRepository resourceAccessRepo;

    @Override
    @Transactional(transactionManager = "multitenantsJpaTransactionManager")
    public List<ResourceMapping> collectResources() {

        final List<ResourceMapping> allResources = new ArrayList<>();

        // For each microservice send a /resources requests
        for (final String service : discoveryClient.getServices()) {
            if (!service.equals("rs-gateway")) {
                final List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (!instances.isEmpty()) {
                    try {
                        final List<ResourceMapping> serviceResources = IResourcesClient
                                .build(new TokenClientProvider<>(IResourcesClient.class,
                                        instances.get(0).getUri().toString()))
                                .getResources();
                        if (serviceResources != null) {
                            saveResources(serviceResources, service);
                            allResources.addAll(serviceResources);
                        }
                    } catch (final FeignException e) {
                        LOG.error("Error getting resources from service " + service, e);
                    }
                }
            }
        }
        return allResources;
    }

    @Override
    public List<ResourcesAccess> retrieveRessources() {
        final Iterable<ResourcesAccess> results = resourceAccessRepo.findAll();
        final List<ResourcesAccess> result = new ArrayList<>();
        results.forEach(t -> {
            result.add(t);
        });
        return result;
    }

    /**
     *
     * Saves resourceMapping into ResourcesAccess repository
     *
     * @param pResourceMappings
     *            resource
     * @param pMicroservice
     *            microservice owner of the resource
     * @since 1.0-SNAPSHOT
     */
    private void saveResources(final List<ResourceMapping> pResourceMappings, final String pMicroservice) {
        final List<ResourcesAccess> resources = new ArrayList<>();
        for (final ResourceMapping resourceMapping : pResourceMappings) {
            final ResourcesAccess resource = resourceAccessRepo
                    .findOneByMicroserviceAndResourceAndVerb(pMicroservice, resourceMapping.getFullPath(),
                                                             HttpVerb.valueOf(resourceMapping.getMethod().name()));
            if (resource == null) {
                resources.add(new ResourcesAccess(resourceMapping.getResourceAccess().description(), pMicroservice,
                        resourceMapping.getFullPath(), HttpVerb.valueOf(resourceMapping.getMethod().name())));
            } else {
                resource.setDescription(resourceMapping.getResourceAccess().description());
                resources.add(resource);
            }
        }
        resourceAccessRepo.save(resources);
    }

}
