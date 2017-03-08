/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import feign.FeignException;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.client.IResourcesClient;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 *
 * Class ResourceService
 *
 * Business service for Resources entities
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @author Sylvain Vissiere-Guerinet
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
     * JWT Security service
     */
    private final JWTService jwtService;

    /**
     * Tenant resolver to identify configured tenants
     */
    private final ITenantResolver tenantResolver;

    /**
     * Enable to init a feign client programmatically
     */
    private final FeignSecurityManager feignSecurityManager;

    public ResourcesService(@Value("${spring.application.name}") String pMicroserviceName,
            DiscoveryClient pDiscoveryClient, IResourcesAccessRepository pResourceAccessRepo, IRoleService pRoleService,
            JWTService pJwtService, ITenantResolver pTenantResolver, FeignSecurityManager pFeignSecurityManager) {
        super();
        microserviceName = pMicroserviceName;
        discoveryClient = pDiscoveryClient;
        resourceAccessRepo = pResourceAccessRepo;
        roleService = pRoleService;
        jwtService = pJwtService;
        tenantResolver = pTenantResolver;
        this.feignSecurityManager = pFeignSecurityManager;
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
                jwtService.injectToken(tenant, RoleAuthority.getSysRole("rs-admin"), "");
                // Collect resources for each tenant configured
                for (final String service : discoveryClient.getServices()) {
                    registerResources(getRemoteResources(service), service);
                }
            }
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public Page<ResourcesAccess> retrieveRessources(final Pageable pPageable) {
        Page<ResourcesAccess> results;
        final String roleName = jwtService.getActualRole();
        // If role is System role or InstanceAdminRole retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName)
                || RoleAuthority.isProjectAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            results = resourceAccessRepo.findAll(pPageable);
        } else {
            // Else retrieve only accessible resources
            final Role currentRole;
            try {
                currentRole = roleService.retrieveRole(roleName);
                final Set<Role> roles = roleService.retrieveInheritedRoles(currentRole);
                List<ResourcesAccess> accessibleResourcesAccesses = Lists.newArrayList(getResourcesAccesses(roles));
                results = new PageImpl<>(accessibleResourcesAccesses, pPageable, accessibleResourcesAccesses.size());
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                results = new PageImpl<>(new ArrayList<>(), pPageable, 0);
            }
        }
        return results;
    }

    @Override
    public List<ResourcesAccess> retrieveRessources() {
        List<ResourcesAccess> results;
        final String roleName = jwtService.getActualRole();
        // If role is System role or InstanceAdminRole retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName)
                || RoleAuthority.isProjectAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            results = resourceAccessRepo.findAll();
        } else {
            // Else retrieve only accessible resources
            final Role currentRole;
            try {
                currentRole = roleService.retrieveRole(roleName);
                final Set<Role> roles = roleService.retrieveInheritedRoles(currentRole);
                results = Lists.newArrayList(getResourcesAccesses(roles));
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                results = new ArrayList<>();
            }
        }
        return results;
    }

    @Override
    public ResourcesAccess retrieveRessource(final Long pResourceId) throws EntityNotFoundException {
        final ResourcesAccess result = resourceAccessRepo.findOne(pResourceId);
        if (result == null) {
            throw new EntityNotFoundException(pResourceId, ResourcesAccess.class);
        }
        return result;
    }

    @Override
    public ResourcesAccess updateResource(final ResourcesAccess pResourceToUpdate) throws EntityNotFoundException {
        if (resourceAccessRepo.exists(pResourceToUpdate.getId())) {
            throw new EntityNotFoundException(pResourceToUpdate.getId(), ResourcesAccess.class);
        }
        return resourceAccessRepo.save(pResourceToUpdate);
    }

    @Override
    public Page<ResourcesAccess> retrieveMicroserviceRessources(final String pMicroserviceName,
            final Pageable pPageable) throws EntityNotFoundException {
        Page<ResourcesAccess> results;
        final String roleName = jwtService.getActualRole();
        // If role is System role or InstanceAdminRole or ProjectAdmin role retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName)
                || RoleAuthority.isProjectAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            results = resourceAccessRepo.findByMicroservice(pMicroserviceName, pPageable);
        } else {
            // Else retrieve only accessible resources
            final Role currentRole;

            currentRole = roleService.retrieveRole(roleName);
            final Set<Role> roles = roleService.retrieveInheritedRoles(currentRole);
            Set<ResourcesAccess> accessibleResourcesAccesses = getResourcesAccesses(roles);
            // filter to get those for the given microservice and convert as a list for the page
            List<ResourcesAccess> accessibleResourcesAccessesForMicroservice = accessibleResourcesAccesses.stream()
                    .filter(ra -> ra.getMicroservice().equals(pMicroserviceName)).collect(Collectors.toList());
            results = new PageImpl<>(accessibleResourcesAccessesForMicroservice, pPageable,
                    accessibleResourcesAccessesForMicroservice.size());

        }
        return results;
    }

    /**
     * Retrieve all the resource accesses contained by a set of roles
     *
     * @param pRoles
     * @return all the resource accesses contained by roles in pRoles
     */
    private Set<ResourcesAccess> getResourcesAccesses(Set<Role> pRoles) {
        return pRoles.stream().flatMap(role -> role.getPermissions().stream()).collect(Collectors.toSet());
    }

    @Override
    public void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName) {
        final List<ResourcesAccess> resources = new ArrayList<>();

        // Retrieve already configured resources for the given microservice
        final List<ResourcesAccess> existingResources = resourceAccessRepo.findByMicroservice(pMicroserviceName);

        pResourcesToRegister.forEach(r -> resources.add(createDefaultResourceConfiguration(r, pMicroserviceName)));

        final List<ResourcesAccess> newResources = new ArrayList<>();
        // Create missing resources
        for (final ResourcesAccess resource : resources) {
            if (!existingResources.contains(resource)) {
                newResources.add(resource);
            }
        }

        // Save missing resources
        if (!newResources.isEmpty()) {
            saveResources(newResources);
        }
    }

    /**
     *
     * Create a {@link ResourcesAccess} with default configuration from a {@link ResourceMapping} object.
     *
     * @param pResource
     *            New resource to configure
     * @param pMicroserviceName
     *            the microservice name
     * @return {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    private ResourcesAccess createDefaultResourceConfiguration(final ResourceMapping pResource,
            final String pMicroserviceName) {
        final ResourcesAccess defaultResource = new ResourcesAccess(pResource, pMicroserviceName);

        if (pResource.getResourceAccess() != null) {
            final String roleName = pResource.getResourceAccess().role().name();
            try {
                final Role role = roleService.retrieveRole(roleName);
                role.addPermission(defaultResource);
            } catch (final EntityNotFoundException e) {
                LOG.debug(e.getMessage(), e);
                LOG.warn("Default role {} for resource {} does not exists.", roleName, defaultResource.getResource());
            }

        }
        return defaultResource;
    }

    /**
     *
     * Return the managed resource list of the given microservice.
     *
     * @param pMicroservice
     *            microservice name
     * @return List of {@link ResourceMapping} of the given microservice
     * @since 1.0-SNAPSHOT
     */
    public List<ResourceMapping> getRemoteResources(final String pMicroservice) {
        // Do not manage local resources
        List<ResourceMapping> remoteResources = new ArrayList<>();
        if (pMicroservice.equals(microserviceName)) {
            return remoteResources;
        }
        try {
            final List<ServiceInstance> instances = discoveryClient.getInstances(pMicroservice);
            if (!instances.isEmpty()) {
                IResourcesClient resourcesClient = FeignClientBuilder.build(new TokenClientProvider<>(
                        IResourcesClient.class, instances.get(0).getUri().toString(), feignSecurityManager));
                remoteResources = resourcesClient.getResources();
            }
        } catch (final FeignException e) {
            LOG.error("Error getting resources from service " + pMicroservice, e);
        }
        return remoteResources;
    }

    /**
     *
     * Save given ResourcesAccess to repository
     *
     * @param pResourcesToSave
     *            entities to save
     * @return saved entities.
     * @since 1.0-SNAPSHOT
     */
    private List<ResourcesAccess> saveResources(final List<ResourcesAccess> pResourcesToSave) {
        return resourceAccessRepo.save(pResourcesToSave);
    }

    @Override
    public void removeRoleResourcesAccess(Long pRoleId, Long pResourcesAccessId)
            throws EntityNotFoundException, EntityOperationForbiddenException {
        Role role = roleService.retrieveRole(pRoleId);
        ResourcesAccess resourcesAccess = retrieveRessource(pResourcesAccessId);
        roleService.removeResourcesAccesses(role, resourcesAccess);
    }

}
