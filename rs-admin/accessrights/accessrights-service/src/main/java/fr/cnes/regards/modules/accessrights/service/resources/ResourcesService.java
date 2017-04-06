/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
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
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
@Service
public class ResourcesService implements IResourcesService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesService.class);

    /**
     * JPA Repository
     */
    private final IResourcesAccessRepository resourceAccessRepo;

    /**
     * Service to manage Role entities
     */
    private final IRoleService roleService;

    public ResourcesService(IResourcesAccessRepository pResourceAccessRepo, IRoleService pRoleService) {
        super();
        resourceAccessRepo = pResourceAccessRepo;
        roleService = pRoleService;
    }

    @Override
    public Page<ResourcesAccess> retrieveRessources(final String pMicroserviceName, final Pageable pPageable)
            throws ModuleException {
        Page<ResourcesAccess> results;
        final String roleName = SecurityUtils.getActualRole();
        // If role is System role or InstanceAdminRole retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName)
                || RoleAuthority.isProjectAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            if (pMicroserviceName == null) {
                results = resourceAccessRepo.findAll(pPageable);
            } else {
                results = resourceAccessRepo.findByMicroservice(pMicroserviceName, pPageable);
            }
        } else {
            // Else retrieve only accessible resources
            final Role currentRole = roleService.retrieveRole(roleName);
            List<ResourcesAccess> accessibleResourcesAccesses = Lists.newArrayList(currentRole.getPermissions());
            results = new PageImpl<>(accessibleResourcesAccesses, pPageable, accessibleResourcesAccesses.size());
        }
        return results;
    }

    @Override
    public ResourcesAccess retrieveRessource(final Long pResourceId) throws ModuleException {
        final ResourcesAccess result = resourceAccessRepo.findOne(pResourceId);
        if (result == null) {
            throw new EntityNotFoundException(pResourceId, ResourcesAccess.class);
        }
        return result;
    }

    @Override
    public ResourcesAccess updateResource(final ResourcesAccess pResourceToUpdate) throws ModuleException {
        if (resourceAccessRepo.exists(pResourceToUpdate.getId())) {
            throw new EntityNotFoundException(pResourceToUpdate.getId(), ResourcesAccess.class);
        }
        return resourceAccessRepo.save(pResourceToUpdate);
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
            resourceAccessRepo.save(newResources);
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

    @Override
    public void removeRoleResourcesAccess(Long pRoleId, Long pResourcesAccessId) throws ModuleException {
        Role role = roleService.retrieveRole(pRoleId);
        ResourcesAccess resourcesAccess = retrieveRessource(pResourcesAccessId);
        roleService.removeResourcesAccesses(role, resourcesAccess);
    }

}
