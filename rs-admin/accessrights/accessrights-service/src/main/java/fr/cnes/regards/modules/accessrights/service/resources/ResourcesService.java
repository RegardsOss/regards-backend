/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Class ResourceService
 * <p>
 * Business service for Resources entities
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
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

    /**
     * Authentication resolver
     */
    private final IAuthenticationResolver authResolver;

    /**
     * Constructor
     */
    public ResourcesService(IResourcesAccessRepository resourceAccessRepo,
                            IRoleService roleService,
                            IAuthenticationResolver authResolver) {
        this.resourceAccessRepo = resourceAccessRepo;
        this.roleService = roleService;
        this.authResolver = authResolver;
    }

    @Override
    public Page<ResourcesAccess> retrieveRessources(String microserviceName, Pageable pageable) throws ModuleException {
        Page<ResourcesAccess> results;
        String roleName = authResolver.getRole();
        // If role is System role or InstanceAdminRole retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            if (microserviceName == null) {
                results = resourceAccessRepo.findAll(pageable);
            } else {
                results = resourceAccessRepo.findByMicroservice(microserviceName, pageable);
            }
        } else if (RoleAuthority.isProjectAdminRole(roleName)) {
            if (microserviceName == null) {
                results = resourceAccessRepo.findByDefaultRoleNot(DefaultRole.INSTANCE_ADMIN, pageable);
            } else {
                results = resourceAccessRepo.findByMicroserviceAndDefaultRoleNot(microserviceName,
                                                                                 DefaultRole.INSTANCE_ADMIN,
                                                                                 pageable);
            }
        } else {
            // Else retrieve only accessible resources
            Role currentRole = roleService.retrieveRole(roleName);
            List<ResourcesAccess> accessibleResourcesAccesses = Lists.newArrayList(currentRole.getPermissions());
            results = new PageImpl<>(accessibleResourcesAccesses, pageable, accessibleResourcesAccesses.size());
        }
        return results;
    }

    @Override
    public ResourcesAccess retrieveRessource(Long resourceId) throws ModuleException {
        Optional<ResourcesAccess> resultOpt = resourceAccessRepo.findById(resourceId);
        if (!resultOpt.isPresent()) {
            throw new EntityNotFoundException(resourceId, ResourcesAccess.class);
        }
        return resultOpt.get();
    }

    @Override
    public ResourcesAccess updateResource(ResourcesAccess resourceToUpdate) throws ModuleException {
        if (resourceAccessRepo.existsById(resourceToUpdate.getId())) {
            throw new EntityNotFoundException(resourceToUpdate.getId(), ResourcesAccess.class);
        }
        return resourceAccessRepo.save(resourceToUpdate);
    }

    @Override
    public void registerResources(List<ResourceMapping> resourcesToRegister, String microserviceName)
        throws ModuleException {

        // Compute resource to register as ResourcesAccess
        List<ResourcesAccess> resources = new ArrayList<>();
        for (ResourceMapping rm : resourcesToRegister) {
            if (rm != null) {
                ResourcesAccess access = new ResourcesAccess();
                access.setControllerSimpleName(rm.getControllerSimpleName());
                if (rm.getResourceAccess() != null) {
                    access.setDefaultRole(rm.getResourceAccess().role());
                    access.setDescription(rm.getResourceAccess().description());
                } else {
                    // Fallback to default
                    access.setDefaultRole(DefaultRole.PROJECT_ADMIN);
                    access.setDescription("Missing description");
                }
                access.setMicroservice(microserviceName);
                access.setResource(rm.getFullPath());
                access.setVerb(rm.getMethod());
                resources.add(access);
            } else {
                LOG.debug("Resource mapping is null!");
            }
        }

        // Retrieve already configured resources for the given microservice
        List<ResourcesAccess> existingResources = resourceAccessRepo.findByMicroservice(microserviceName);

        // Extract and save new resources
        List<ResourcesAccess> newResources = new ArrayList<>();
        for (ResourcesAccess ra : resources) {
            if (!existingResources.contains(ra)) {
                newResources.add(ra);
            }
        }
        if (!newResources.isEmpty()) {
            resourceAccessRepo.saveAll(newResources);
        }

        // Compute map by native roles
        Map<DefaultRole, Set<ResourcesAccess>> accessesByDefaultRole = new EnumMap<>(DefaultRole.class);
        for (ResourcesAccess nra : newResources) {
            Set<ResourcesAccess> set = accessesByDefaultRole.computeIfAbsent(nra.getDefaultRole(),
                                                                             k -> new HashSet<>());
            set.add(nra);
        }

        // Link new resources with existing roles
        for (Map.Entry<DefaultRole, Set<ResourcesAccess>> entry : accessesByDefaultRole.entrySet()) {
            Role role = roleService.retrieveRole(entry.getKey().toString());
            roleService.addResourceAccesses(role.getId(), entry.getValue().toArray(new ResourcesAccess[0]));
        }
    }

    @Override
    public List<ResourcesAccess> retrieveMicroserviceControllerEndpoints(String microserviceName,
                                                                         String controllerName,
                                                                         String roleName) {
        if (RoleAuthority.isInstanceAdminRole(roleName)
            || RoleAuthority.isProjectAdminRole(roleName)
            || RoleAuthority.isSysRole(roleName)) {
            // No restriction for virtual role
            return resourceAccessRepo.findByMicroserviceAndControllerSimpleNameAndDefaultRoleNotOrderByResource(
                microserviceName,
                controllerName,
                DefaultRole.INSTANCE_ADMIN);
        }
        return resourceAccessRepo.findManageableResources(microserviceName, controllerName, roleName);
    }

    @Override
    public List<String> retrieveMicroserviceControllers(String microserviceName, String roleName) {
        if (RoleAuthority.isInstanceAdminRole(roleName)
            || RoleAuthority.isProjectAdminRole(roleName)
            || RoleAuthority.isSysRole(roleName)) {
            // No restriction for virtual role
            return resourceAccessRepo.findAllControllersByMicroservice(microserviceName);
        }
        return resourceAccessRepo.findManageableControllers(microserviceName, roleName);
    }

    @Override
    public void removeRoleResourcesAccess(String roleName, Long resourcesAccessId) throws ModuleException {
        ResourcesAccess resourcesAccess = retrieveRessource(resourcesAccessId);
        roleService.removeResourcesAccesses(roleName, resourcesAccess);
    }

}
