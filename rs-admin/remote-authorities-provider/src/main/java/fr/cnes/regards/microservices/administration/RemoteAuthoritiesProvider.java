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
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContextException;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRoleResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class MicroserviceAuthoritiesProvider
 *
 * IAuthoritiesProvider implementation for all microservices exception administration.
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
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
    private final IRolesClient roleClient;

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     *
     * Constructor
     *
     * @param pRolesClient
     *            Feign client to query administration service for roles
     * @param pResourcesclient
     *            Feign client to query administration service for resources
     * @param runtimeTenantResolver
     *            runtime tenant resolver
     * @since 1.0-SNAPSHOT
     */
    public RemoteAuthoritiesProvider(final DiscoveryClient discoveryClient,
            final IMicroserviceResourceClient pResourcesclient, final IRolesClient pRolesClient,
            final IRuntimeTenantResolver runtimeTenantResolver, final IRoleResourceClient pRoleResourceClient) {
        super(discoveryClient);
        resourcesClient = pResourcesclient;
        roleClient = pRolesClient;
        roleResourceClient = pRoleResourceClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void registerEndpoints(final String microserviceName, final String tenant,
            final List<ResourceMapping> localEndpoints) {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Registering endpoints for tenant {}", tenant);

        // Register endpoints to administration service and retrieve configured ones
        FeignSecurityManager.asSystem();
        final ResponseEntity<Void> response = resourcesClient.registerMicroserviceEndpoints(microserviceName,
                                                                                            localEndpoints);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new ApplicationContextException("Error registering endpoints to administration service");
        }
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities(final String microserviceName, final String tenant) {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Retrieving role authorities for tenant {}", tenant);

        final List<RoleAuthority> roleAuths = new ArrayList<>();

        FeignSecurityManager.asSystem();
        final ResponseEntity<List<Resource<Role>>> result = roleClient.getAllRoles();

        if (result.getStatusCode().equals(HttpStatus.OK)) {
            final List<Resource<Role>> body = result.getBody();
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
        ResponseEntity<List<Resource<ResourcesAccess>>> resourcesResponse = roleResourceClient
                .getRoleResources(roleName);
        if (resourcesResponse.getStatusCode().equals(HttpStatus.OK)) {
            final List<Resource<ResourcesAccess>> body = resourcesResponse.getBody();
            final List<ResourcesAccess> resources = HateoasUtils.unwrapList(body);
            return resources.stream().filter(resource -> resource.getMicroservice().equals(microserviceName))
//                    .peek((resource) -> LOGGER.info("Building resource mapping of {}", resource.toString()))
                    .map(resource -> buildResourceMapping(resource, Collections.singleton(new Role(roleName))))
                    .collect(Collectors.toSet());
        }
        LOGGER.warn("Role {} seems to have been deleted. We are skipping the resource update", roleName);
        return Sets.newHashSet();
    }

    /**
     *
     * Create a {@link RoleAuthority} from a {@link Role}
     *
     * @param pRole
     *            role to convert to RoleAuthority
     * @return {@link RoleAuthority}
     * @since 1.0-SNAPSHOT
     */
    private RoleAuthority createRoleAuthority(final Role pRole) {
        final RoleAuthority roleAuth = new RoleAuthority(pRole.getName());
        roleAuth.setAuthorizedIpAdresses(pRole.getAuthorizedAddresses());
        return roleAuth;
    }

    private ResourceMapping buildResourceMapping(final ResourcesAccess pRa, final Collection<Role> pRoles) {
        final ResourceMapping mapping = new ResourceMapping(
                ResourceAccessAdapter.createResourceAccess(pRa.getDescription(), null), pRa.getResource(),
                pRa.getControllerSimpleName(), RequestMethod.valueOf(pRa.getVerb().toString()));
        mapping.setAutorizedRoles(pRoles.stream().map(role -> new RoleAuthority(role.getName()))
                .collect(Collectors.toList()));
        return mapping;
    }
}
