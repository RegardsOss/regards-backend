/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import feign.FeignException;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.client.IResourcesClient;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.client.TokenClientProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class ResourceService
 *
 * Business service for Resources entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service
public class ResourcesService implements IResourcesService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesService.class);

    /**
     * Current microservice name
     */

    private final String microserviceName;

    /**
     * Eureka discovery client to access other microservices.
     */
    private final DiscoveryClient discoveryClient;

    /**
     * JPA Repository
     */
    private final IResourcesAccessRepository resourceAccessRepo;

    /**
     * Service to manage Role entities
     */
    private final IRoleService roleService;

    /**
     * Authorization service
     */
    private final MethodAuthorizationService securityService;

    /**
     * JWT Security service
     */
    private final JWTService jwtService;

    /**
     * Tenant resolver to identify configured tenants
     */
    private final ITenantResolver tenantResolver;

    public ResourcesService(@Value("${spring.application.name}") final String pMicroserviceName,
            final DiscoveryClient pDiscoveryClient, final IResourcesAccessRepository pResourceAccessRepo,
            final IRoleService pRoleService, final MethodAuthorizationService pSecurityService,
            final JWTService pJwtService, final ITenantResolver pTenantResolver) {
        super();
        microserviceName = pMicroserviceName;
        discoveryClient = pDiscoveryClient;
        resourceAccessRepo = pResourceAccessRepo;
        roleService = pRoleService;
        securityService = pSecurityService;
        jwtService = pJwtService;
        tenantResolver = pTenantResolver;
    }

    /**
     *
     * Init resources when microservice starts
     *
     * @since 1.0-SNAPSHOT
     */
    @PostConstruct
    public void init() {
        try {
            for (final String tenant : tenantResolver.getAllTenants()) {
                jwtService.injectToken(tenant, RoleAuthority.getSysRole("rs-admin"));
                // Collect resources for each tenant configured
                this.collectResources();
            }
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    @MultitenantTransactional
    public List<ResourceMapping> collectResources() {

        final List<ResourceMapping> allResources = new ArrayList<>();

        // Get local resources
        allResources.addAll(collectLocalResources());

        // For each microservice collect and save resources
        for (final String service : discoveryClient.getServices()) {
            // Avoid local microservice resources.
            if (!service.equals(microserviceName)) {
                allResources.addAll(collectRemoteResources(service));
            }
        }
        return allResources;
    }

    @Override
    public List<ResourcesAccess> retrieveRessources() {
        final Iterable<ResourcesAccess> results = resourceAccessRepo.findAll();
        final List<ResourcesAccess> result = new ArrayList<>();
        results.forEach(t -> result.add(t));
        return result;
    }

    /**
     *
     * Collect resources from the current microservice
     *
     * @return List<ResourceMapping>
     * @since 1.0+SNAPSHOT
     */
    private List<ResourceMapping> collectLocalResources() {
        // Get local resources
        final List<ResourceMapping> localResources = securityService.getResources();
        // Save resources
        saveResources(localResources, microserviceName);
        return localResources;
    }

    /**
     *
     * Collect resources from a remote microservice and save results.
     *
     * @param pMicroservice
     *            microservice to collect.
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    private List<ResourceMapping> collectRemoteResources(final String pMicroservice) {
        final List<ResourceMapping> serviceResources = getRemoteResources(pMicroservice);
        saveResources(serviceResources, pMicroservice);
        return serviceResources;

    }

    public List<ResourceMapping> getRemoteResources(final String pMicroservice) {
        List<ResourceMapping> remoteResources = new ArrayList<>();
        try {
            final List<ServiceInstance> instances = discoveryClient.getInstances(pMicroservice);
            if (!instances.isEmpty()) {
                remoteResources = IResourcesClient
                        .build(new TokenClientProvider<>(IResourcesClient.class, instances.get(0).getUri().toString()))
                        .getResources();
            }
        } catch (final FeignException e) {
            LOG.error("Error getting resources from service " + pMicroservice, e);
        }
        return remoteResources;
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
    private List<ResourcesAccess> saveResources(final List<ResourceMapping> pResourceMappings,
            final String pMicroservice) {

        // Retrieve all already existing resources for the given microservice
        final List<ResourcesAccess> existingResources = resourceAccessRepo.findByMicroservice(pMicroservice);
        final List<ResourcesAccess> collectedResources = new ArrayList<>();

        // Create ResourcesAccess from RequestMappings for all collected resource
        for (final ResourceMapping collectedResource : pResourceMappings) {
            final List<Role> defaultRoles = new ArrayList<>();
            final ResourcesAccess resource = new ResourcesAccess(collectedResource.getResourceAccess().description(),
                    pMicroservice, collectedResource.getFullPath(),
                    HttpVerb.valueOf(collectedResource.getMethod().name()));

            // Add default role if exists
            final Role role = roleService.retrieveRole(collectedResource.getResourceAccess().role().toString());
            if (role != null) {
                defaultRoles.add(role);
                resource.setRoles(defaultRoles);
            }
            collectedResources.add(resource);
        }

        // Merge Existing resources and collected to resource to save new ones and update existing ones.
        for (final ResourcesAccess collectedResource : collectedResources) {
            final int index = existingResources.indexOf(collectedResource);
            if (index >= 0) {
                collectedResource.setId(existingResources.get(index).getId());
                collectedResource.setRoles(existingResources.get(index).getRoles());
            }
        }

        // Save resources
        final List<ResourcesAccess> savedResources = new ArrayList<>();
        resourceAccessRepo.save(collectedResources).forEach(r -> savedResources.add(r));
        return savedResources;
    }

}
