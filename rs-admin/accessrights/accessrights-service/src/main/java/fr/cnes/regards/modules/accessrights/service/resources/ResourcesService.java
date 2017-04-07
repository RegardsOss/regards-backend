/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
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
@MultitenantTransactional
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
            // FIXME retrieve resource by page from repository
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
    public void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName)
            throws ModuleException {

        // Compute resource to register as ResourcesAccess
        List<ResourcesAccess> resources = new ArrayList<>();
        for (ResourceMapping rm : pResourcesToRegister) {
            if (rm != null) {
                ResourcesAccess access = new ResourcesAccess();
                access.setControllerSimpleName(rm.getControllerSimpleName());
                access.setDefaultRole(rm.getResourceAccess().role());
                access.setDescription(rm.getResourceAccess().description());
                access.setMicroservice(pMicroserviceName);
                access.setResource(rm.getFullPath());
                access.setVerb(rm.getMethod());
                resources.add(access);
            } else {
                LOG.debug("Resource mapping is null!");
            }
        }

        // Retrieve already configured resources for the given microservice
        List<ResourcesAccess> existingResources = resourceAccessRepo.findByMicroservice(pMicroserviceName);

        // Extract and save new resources
        List<ResourcesAccess> newResources = new ArrayList<>();
        for (ResourcesAccess ra : resources) {
            if (!existingResources.contains(ra)) {
                newResources.add(ra);
            }
        }
        if (!newResources.isEmpty()) {
            resourceAccessRepo.save(newResources);
        }

        // TODO clean missing resources

        // Compute map by native roles
        Map<DefaultRole, Set<ResourcesAccess>> accessesByDefaultRole = new EnumMap<>(DefaultRole.class);
        for (ResourcesAccess nra : newResources) {
            Set<ResourcesAccess> set = accessesByDefaultRole.get(nra.getDefaultRole());
            if (set == null) {
                set = new HashSet<>();
                accessesByDefaultRole.put(nra.getDefaultRole(), set);
            }
            set.add(nra);
        }

        // Link new resources with existing roles
        for (Map.Entry<DefaultRole, Set<ResourcesAccess>> entry : accessesByDefaultRole.entrySet()) {
            Role role = roleService.retrieveRole(entry.getKey().toString());
            roleService.addResourceAccesses(role.getId(),
                                            entry.getValue().toArray(new ResourcesAccess[entry.getValue().size()]));
        }
    }

    @Override
    public void removeRoleResourcesAccess(Long pRoleId, Long pResourcesAccessId) throws ModuleException {
        Role role = roleService.retrieveRole(pRoleId);
        ResourcesAccess resourcesAccess = retrieveRessource(pResourcesAccessId);
        roleService.removeResourcesAccesses(role, resourcesAccess);
    }

}
