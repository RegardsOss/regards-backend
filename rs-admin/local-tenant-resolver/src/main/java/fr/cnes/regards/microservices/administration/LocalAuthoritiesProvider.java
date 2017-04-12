/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

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
 *
 * Class AuhtoritiesProvider
 *
 * Authorities provider for internal administration microservice access
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
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

    public LocalAuthoritiesProvider(final IRoleService pRoleService, final IResourcesService pResourcesService,
            final IRuntimeTenantResolver runtimeTenantResolver) {
        super();
        roleService = pRoleService;
        resourcesService = pResourcesService;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public List<ResourceMapping> registerEndpoints(String microserviceName, String tenant,
            final List<ResourceMapping> localEndpoints) throws SecurityException {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Registering endpoints for tenant {}", tenant);

        try {
            resourcesService.registerResources(localEndpoints, microserviceName);
        } catch (ModuleException e) {
            throw new SecurityException(e);
        }

        // get a map that for each ResourcesAccess ra links the roles containing ra
        final Set<Role> roles = roleService.retrieveRoles();
        final SetMultimap<ResourcesAccess, Role> multimap = HashMultimap.create();

        roles.forEach(role -> role.getPermissions().forEach(ra -> multimap.put(ra, role)));

        // create ResourceMappings
        return multimap.asMap().entrySet().stream().map(entry -> buildResourceMapping(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

    }

    private ResourceMapping buildResourceMapping(final ResourcesAccess pRa, final Collection<Role> pRoles) {
        final ResourceMapping mapping = new ResourceMapping(
                ResourceAccessAdapter.createResourceAccess(pRa.getDescription(), null), pRa.getResource(),
                pRa.getControllerSimpleName(), RequestMethod.valueOf(pRa.getVerb().toString()));
        mapping.setAutorizedRoles(pRoles.stream().map(role -> new RoleAuthority(role.getName()))
                .collect(Collectors.toList()));
        return mapping;
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities(String microserviceName, String tenant) throws SecurityException {

        // Specified the working tenant
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.debug("Retrieving role authorities for tenant {}", tenant);

        final List<RoleAuthority> results = new ArrayList<>();
        final Set<Role> roles = roleService.retrieveRoles();
        for (final Role role : roles) {
            final RoleAuthority roleAuth = new RoleAuthority(role.getName());
            results.add(roleAuth);
        }
        return results;
    }

}
