/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.ArrayList;
import java.util.List;

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

import feign.FeignException;
import fr.cnes.regards.client.core.TokenClientProvider;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.client.IResourcesClient;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.event.UpdateAuthoritiesEvent;
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
     * AMQP Event publisher.
     */
    private final IPublisher eventPublisher;

    /**
     * Tenant resolver to identify configured tenants
     */
    private final ITenantResolver tenantResolver;

    public ResourcesService(@Value("${spring.application.name}") final String pMicroserviceName,
            final DiscoveryClient pDiscoveryClient, final IResourcesAccessRepository pResourceAccessRepo,
            final IRoleService pRoleService, final JWTService pJwtService, final ITenantResolver pTenantResolver,
            final IPublisher pEventPublisher) {
        super();
        microserviceName = pMicroserviceName;
        discoveryClient = pDiscoveryClient;
        resourceAccessRepo = pResourceAccessRepo;
        roleService = pRoleService;
        jwtService = pJwtService;
        tenantResolver = pTenantResolver;
        eventPublisher = pEventPublisher;
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
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            results = resourceAccessRepo.findAll(pPageable);
        } else {
            // Else retrieve only accessible resources
            Role currentRole;
            try {
                currentRole = roleService.retrieveRole(roleName);
                final List<Role> roles = roleService.retrieveInheritedRoles(currentRole);
                final List<String> rolesName = new ArrayList<>();
                roles.forEach(r -> rolesName.add(r.getName()));
                results = resourceAccessRepo.findDistinctByRolesNameIn(rolesName, pPageable);
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
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            results = resourceAccessRepo.findAll();
        } else {
            // Else retrieve only accessible resources
            Role currentRole;
            try {
                currentRole = roleService.retrieveRole(roleName);
                final List<Role> roles = roleService.retrieveInheritedRoles(currentRole);
                final List<String> rolesName = new ArrayList<>();
                roles.forEach(r -> rolesName.add(r.getName()));
                results = resourceAccessRepo.findDistinctByRolesNameIn(rolesName);
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
            final Pageable pPageable) {
        Page<ResourcesAccess> results;
        final String roleName = jwtService.getActualRole();
        // If role is System role or InstanceAdminRole retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            results = resourceAccessRepo.findByMicroservice(pMicroserviceName, pPageable);
        } else {
            // Else retrieve only accessible resources
            Role currentRole;
            try {
                currentRole = roleService.retrieveRole(roleName);
                final List<Role> roles = roleService.retrieveInheritedRoles(currentRole);
                final List<String> rolesName = new ArrayList<>();
                roles.forEach(r -> rolesName.add(r.getName()));
                results = resourceAccessRepo.findDistinctByMicroserviceAndRolesNameIn(pMicroserviceName, rolesName,
                                                                                      pPageable);
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                results = new PageImpl<>(new ArrayList<>(), pPageable, 0);
            }

        }
        return results;
    }

    @Override
    public void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName) {
        final List<ResourcesAccess> resources = new ArrayList<>();
        pResourcesToRegister.forEach(r -> resources.add(createDefaultResourceConfiguration(r, pMicroserviceName)));

        // Retrieve aleady configured resources for the given microservice
        final List<ResourcesAccess> existingResources = resourceAccessRepo.findByMicroservice(pMicroserviceName);

        final List<ResourcesAccess> newResources = new ArrayList<>();
        // Create missing resources
        for (final ResourcesAccess resource : resources) {
            if (!existingResources.contains(resource)) {
                newResources.add(resource);
            }
        }

        // Save missing resources
        saveResources(newResources);
    }

    /**
     *
     * Create a {@link ResourcesAccess} with default configuration from a {@link ResourceMapping} object.
     *
     * @param pResource
     *            New resource to configure
     * @return {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    private ResourcesAccess createDefaultResourceConfiguration(final ResourceMapping pResource,
            final String pMicroserviceName) {
        final ResourcesAccess defaultResource = new ResourcesAccess(pResource, pMicroserviceName);
        final List<Role> roles = new ArrayList<>();

        if (pResource.getResourceAccess() != null) {
            final String roleName = pResource.getResourceAccess().role().name();
            try {
                final Role role = roleService.retrieveRole(roleName);
                roles.add(role);
            } catch (final EntityNotFoundException e) {
                LOG.debug(e.getMessage(), e);
                LOG.warn("Default role {} for resource {} does not exists.", roleName, defaultResource.getResource());
            }

        }

        defaultResource.setRoles(roles);
        return defaultResource;
    }

    /**
     *
     * Return the resources list of the given microservice.
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
     * Save given ResourcesAccess to repository
     *
     * @param pResourcesToSave
     *            entities to save
     * @return saved entities.
     * @since 1.0-SNAPSHOT
     */
    private List<ResourcesAccess> saveResources(final List<ResourcesAccess> pResourcesToSave) {
        final List<ResourcesAccess> results = new ArrayList<>();

        // First Step is to calculate new associated roles for each resource
        for (final ResourcesAccess resource : pResourcesToSave) {
            calculateResourceInheritedRoles(resource);
        }

        final Iterable<ResourcesAccess> savedResources = resourceAccessRepo.save(pResourcesToSave);
        if (savedResources != null) {
            savedResources.forEach(results::add);
        }
        try {
            eventPublisher.publish(UpdateAuthoritiesEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                   AmqpCommunicationTarget.EXTERNAL);
        } catch (final RabbitMQVhostException e) {
            LOG.error("Error publishing resources updates to all running microservices.");
            LOG.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     *
     * Method to update given resource roles with all inherited roles of each role.
     *
     * @param pResource
     *            resource to update
     * @since 1.0-SNAPSHOT
     */
    private void calculateResourceInheritedRoles(final ResourcesAccess pResource) {

        final List<Role> inheritedRoles = new ArrayList<>();
        for (final Role role : pResource.getRoles()) {
            for (final Role inheritedRole : roleService.retrieveInheritedRoles(role)) {
                inheritedRoles.add(inheritedRole);
            }
        }
        pResource.addRoles(inheritedRoles);

    }

}
