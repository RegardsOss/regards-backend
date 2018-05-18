/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
     *
     * @param pResourceAccessRepo
     * @param pRoleService
     */
    public ResourcesService(final IResourcesAccessRepository pResourceAccessRepo, final IRoleService pRoleService,
            IAuthenticationResolver authResolver) {
        super();
        resourceAccessRepo = pResourceAccessRepo;
        roleService = pRoleService;
        this.authResolver = authResolver;
    }

    @Override
    public Page<ResourcesAccess> retrieveRessources(final String pMicroserviceName, final Pageable pPageable)
            throws ModuleException {
        Page<ResourcesAccess> results;
        final String roleName = authResolver.getRole();
        // If role is System role or InstanceAdminRole retrieve all resources
        if ((roleName == null) || RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isSysRole(roleName)) {
            if (pMicroserviceName == null) {
                results = resourceAccessRepo.findAll(pPageable);
            } else {
                results = resourceAccessRepo.findByMicroservice(pMicroserviceName, pPageable);
            }
        } else if (RoleAuthority.isProjectAdminRole(roleName)) {
            if (pMicroserviceName == null) {
                results = resourceAccessRepo.findByDefaultRoleNot(DefaultRole.INSTANCE_ADMIN, pPageable);
            } else {
                results = resourceAccessRepo.findByMicroserviceAndDefaultRoleNot(pMicroserviceName,
                                                                                 DefaultRole.INSTANCE_ADMIN, pPageable);
            }
        } else {
            // FIXME retrieve resource by page from repository
            // Else retrieve only accessible resources
            final Role currentRole = roleService.retrieveRole(roleName);
            final List<ResourcesAccess> accessibleResourcesAccesses = Lists.newArrayList(currentRole.getPermissions());
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
        final List<ResourcesAccess> resources = new ArrayList<>();
        for (final ResourceMapping rm : pResourcesToRegister) {
            if (rm != null) {
                final ResourcesAccess access = new ResourcesAccess();
                access.setControllerSimpleName(rm.getControllerSimpleName());
                if (rm.getResourceAccess() != null) {
                    access.setDefaultRole(rm.getResourceAccess().role());
                    access.setDescription(rm.getResourceAccess().description());
                } else {
                    // Fallback to default
                    access.setDefaultRole(DefaultRole.PROJECT_ADMIN);
                    access.setDescription("Missing description");
                }
                access.setMicroservice(pMicroserviceName);
                access.setResource(rm.getFullPath());
                access.setVerb(rm.getMethod());
                resources.add(access);
            } else {
                LOG.debug("Resource mapping is null!");
            }
        }

        // Retrieve already configured resources for the given microservice
        final List<ResourcesAccess> existingResources = resourceAccessRepo.findByMicroservice(pMicroserviceName);

        // Extract and save new resources
        final List<ResourcesAccess> newResources = new ArrayList<>();
        for (final ResourcesAccess ra : resources) {
            if (!existingResources.contains(ra)) {
                newResources.add(ra);
            }
        }
        if (!newResources.isEmpty()) {
            resourceAccessRepo.save(newResources);
        }

        // TODO clean missing resources

        // Compute map by native roles
        final Map<DefaultRole, Set<ResourcesAccess>> accessesByDefaultRole = new EnumMap<>(DefaultRole.class);
        for (final ResourcesAccess nra : newResources) {
            Set<ResourcesAccess> set = accessesByDefaultRole.get(nra.getDefaultRole());
            if (set == null) {
                set = new HashSet<>();
                accessesByDefaultRole.put(nra.getDefaultRole(), set);
            }
            set.add(nra);
        }

        // Link new resources with existing roles
        for (final Map.Entry<DefaultRole, Set<ResourcesAccess>> entry : accessesByDefaultRole.entrySet()) {
            final Role role = roleService.retrieveRole(entry.getKey().toString());
            roleService.addResourceAccesses(role.getId(),
                                            entry.getValue().toArray(new ResourcesAccess[entry.getValue().size()]));
        }
    }

    @Override
    public List<ResourcesAccess> retrieveMicroserviceControllerEndpoints(final String pMicroserviceName,
            final String pControllerName, String roleName) {
        if (RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isProjectAdminRole(roleName)
                || RoleAuthority.isSysRole(roleName)) {
            // No restriction for virtual role
            return resourceAccessRepo
                    .findByMicroserviceAndControllerSimpleNameAndDefaultRoleNotOrderByResource(pMicroserviceName,
                                                                                               pControllerName,
                                                                                               DefaultRole.INSTANCE_ADMIN);
        }
        return resourceAccessRepo.findManageableResources(pMicroserviceName, pControllerName, roleName);
    }

    @Override
    public List<String> retrieveMicroserviceControllers(final String pMicroserviceName, String roleName) {
        if (RoleAuthority.isInstanceAdminRole(roleName) || RoleAuthority.isProjectAdminRole(roleName)
                || RoleAuthority.isSysRole(roleName)) {
            // No restriction for virtual role
            return resourceAccessRepo.findAllControllersByMicroservice(pMicroserviceName);
        }
        return resourceAccessRepo.findManageableControllers(pMicroserviceName, roleName);
    }

    @Override
    public void removeRoleResourcesAccess(final String pRoleName, final Long pResourcesAccessId)
            throws ModuleException {
        final ResourcesAccess resourcesAccess = retrieveRessource(pResourcesAccessId);
        roleService.removeResourcesAccesses(pRoleName, resourcesAccess);
    }

}
