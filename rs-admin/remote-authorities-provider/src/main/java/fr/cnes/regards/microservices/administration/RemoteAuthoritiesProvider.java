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
package fr.cnes.regards.microservices.administration;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.client.CacheableRolesClient;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRoleResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class MicroserviceAuthoritiesProvider
 * <p>
 * IAuthoritiesProvider implementation for all microservices exception administration.
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
public class RemoteAuthoritiesProvider extends AbstractProjectDiscoveryClientChecker implements IAuthoritiesProvider {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAuthoritiesProvider.class);

    /**
     * Administration microservice REST client
     */
    private final IMicroserviceResourceClient resourcesClient;

    private final IRoleResourceClient roleResourceClient;

    /**
     * Administration microservice REST client
     */
    private final CacheableRolesClient cacheRoleClient;

    private final IRolesClient roleClient;

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Constructor
     */
    public RemoteAuthoritiesProvider(final DiscoveryClient discoveryClient,
                                     final IMicroserviceResourceClient resourcesclient,
                                     final CacheableRolesClient cacheableRolesClient,
                                     final IRolesClient rolesClient,
                                     final IRuntimeTenantResolver runtimeTenantResolver,
                                     final IRoleResourceClient roleResourceClient) {
        super(discoveryClient);
        resourcesClient = resourcesclient;
        roleClient = rolesClient;
        cacheRoleClient = cacheableRolesClient;
        this.roleResourceClient = roleResourceClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void registerEndpoints(final String microserviceName,
                                  final String tenant,
                                  final List<ResourceMapping> localEndpoints) throws SecurityException {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Registering endpoints for tenant {}", tenant);

        // Register endpoints to administration service and retrieve configured ones
        FeignSecurityManager.asSystem();
        try {
            final ResponseEntity<Void> response = resourcesClient.registerMicroserviceEndpoints(microserviceName,
                                                                                                localEndpoints);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                throw new SecurityException(String.format(
                    "Error registering endpoints to administration service for tenant %s",
                    tenant));
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new SecurityException(String.format(
                "Error registering endpoints to administration service for tenant %s. Cause : ",
                tenant,
                e.getMessage()), e);
        }

    }

    @Override
    public boolean shouldAccessToResourceRequiring(String roleName) {
        ResponseEntity<Boolean> response;
        try {
            response = cacheRoleClient.shouldAccessToResourceRequiring(roleName);
            if ((response != null) && response.hasBody()) {
                return response.getBody();
            }
        } catch (EntityNotFoundException e) {
            // Nothing  to do. Role does not exists so acccess is denied
        }
        return false;

    }

    @Override
    public List<RoleAuthority> getRoleAuthorities(final String microserviceName, final String tenant) {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Retrieving role authorities for tenant {}", tenant);

        final List<RoleAuthority> roleAuths = new ArrayList<>();

        FeignSecurityManager.asSystem();
        final ResponseEntity<List<EntityModel<Role>>> result = roleClient.getAllRoles();

        if (result.getStatusCode().equals(HttpStatus.OK)) {
            final List<EntityModel<Role>> body = result.getBody();
            if (body != null) {
                final List<Role> roles = HateoasUtils.unwrapList(body);
                for (final Role role : roles) {
                    roleAuths.add(createRoleAuthority(role));
                }
            }
        }
        return roleAuths;
    }

    @Override
    public Set<ResourceMapping> getResourceMappings(String microserviceName, String tenant, String roleName) {
        runtimeTenantResolver.forceTenant(tenant);
        // lets get the role from distant admin
        FeignSecurityManager.asSystem();
        ResponseEntity<List<EntityModel<ResourcesAccess>>> resourcesResponse = roleResourceClient.getRoleResourcesForMicroservice(
            roleName,
            microserviceName);
        if (resourcesResponse.getStatusCode().equals(HttpStatus.OK)) {
            final List<EntityModel<ResourcesAccess>> body = resourcesResponse.getBody();
            final List<ResourcesAccess> resources = HateoasUtils.unwrapList(body);
            return resources.stream()
                .map(resource -> buildResourceMapping(resource, Collections.singleton(new Role(roleName))))
                .collect(Collectors.toSet());
        }
        LOGGER.warn("Role {} seems to have been deleted. We are skipping the resource update", roleName);
        return Sets.newHashSet();
    }

    /**
     * Create a {@link RoleAuthority} from a {@link Role}
     *
     * @param pRole role to convert to RoleAuthority
     * @return {@link RoleAuthority}
     */
    private RoleAuthority createRoleAuthority(final Role pRole) {
        final RoleAuthority roleAuth = new RoleAuthority(pRole.getName());
        roleAuth.setAuthorizedIpAdresses(pRole.getAuthorizedAddresses());
        return roleAuth;
    }

    private ResourceMapping buildResourceMapping(final ResourcesAccess pRa, final Collection<Role> pRoles) {
        final ResourceMapping mapping = new ResourceMapping(ResourceAccessAdapter.createResourceAccess(pRa.getDescription(),
                                                                                                       null),
                                                            pRa.getResource(),
                                                            pRa.getControllerSimpleName(),
                                                            RequestMethod.valueOf(pRa.getVerb().toString()));
        mapping.setAutorizedRoles(pRoles.stream()
                                      .map(role -> new RoleAuthority(role.getName()))
                                      .collect(Collectors.toList()));
        return mapping;
    }
}
