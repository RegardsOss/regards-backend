/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microservices.administration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Class AuhtoritiesProvider
 *
 * Authorities provider for internal administration microservice access
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
public class LocalAuthoritiesProvider implements IAuthoritiesProvider {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalAuthoritiesProvider.class);

    /**
     * Role service
     */
    private final IRoleService roleService;

    /**
     * Resources service
     */
    private final IResourcesService resourcesService;

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public LocalAuthoritiesProvider(IRoleService roleService, IResourcesService resourcesService,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.roleService = roleService;
        this.resourcesService = resourcesService;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void registerEndpoints(String microserviceName, String tenant, List<ResourceMapping> localEndpoints)
            throws SecurityException {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Registering endpoints for tenant {}", tenant);

        try {
            resourcesService.registerResources(localEndpoints, microserviceName);
        } catch (ModuleException e) {
            throw new SecurityException(e);
        }
    }

    private ResourceMapping buildResourceMapping(ResourcesAccess resourcesAccess, Collection<Role> roles) {
        ResourceMapping mapping = new ResourceMapping(
                ResourceAccessAdapter.createResourceAccess(resourcesAccess.getDescription(), null),
                resourcesAccess.getResource(), resourcesAccess.getControllerSimpleName(),
                RequestMethod.valueOf(resourcesAccess.getVerb().toString()));
        mapping.setAutorizedRoles(roles.stream().map(role -> new RoleAuthority(role.getName()))
                .collect(Collectors.toList()));
        return mapping;
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities(String microserviceName, String tenant) throws SecurityException {
        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Retrieving role authorities for tenant {}", tenant);

        return roleService.retrieveRoles().stream().map(role -> new RoleAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<ResourceMapping> getResourceMappings(String microserviceName, String tenant, String roleName) {
        runtimeTenantResolver.forceTenant(tenant);

        try {
            Role role = roleService.retrieveRole(roleName);
            return role.getPermissions().stream()
                    .filter(resource -> resource.getMicroservice().equals(microserviceName))
                    .map(resource -> buildResourceMapping(resource, Collections.singleton(role)))
                    .collect(Collectors.toSet());
        } catch (EntityNotFoundException e) {
            LOGGER.warn("Role {} seems to have been deleted. We are skipping the resource update", roleName);
            LOGGER.trace("Role not found", e);
            return Sets.newHashSet();
        }
    }

    @Override
    public boolean shouldAccessToResourceRequiring(String roleName) {
        try {
            return roleService.isCurrentRoleSuperiorTo(roleName);
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
        return false;
    }

}
