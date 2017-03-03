/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
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
 * @since 1.0-SNAPSHOT
 */
public class LocalAuthoritiesProvider implements IAuthoritiesProvider {

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * Role service
     */
    private final IRoleService roleService;

    /**
     * Resources service
     */
    private final IResourcesService resourcesService;

    public LocalAuthoritiesProvider(@Value("${spring.application.name") final String pMicroserviceName,
            final IRoleService pRoleService, final IResourcesService pResourcesService) {
        super();
        microserviceName = pMicroserviceName;
        roleService = pRoleService;
        resourcesService = pResourcesService;
    }

    /**
     *
     * Save new endpoints with default configuration if they doesn't exists and return configured endpoints of the
     * administration microservice
     *
     * @param pLocalEndpoints
     *            Collected endpoints with default configuration
     * @return All configured endpoints of the administration microservice
     * @since 1.0-SNAPSHOT
     */
    @Override
    public List<ResourceMapping> registerEndpoints(final List<ResourceMapping> pLocalEndpoints) {
        resourcesService.registerResources(pLocalEndpoints, microserviceName);
        // get a map that for each ResourcesAccess ra links the roles containing ra
        Set<Role> roles = roleService.retrieveRoles();
        SetMultimap<ResourcesAccess, Role> multimap = HashMultimap.create();

        roles.forEach(role -> role.getPermissions().forEach(ra -> multimap.put(ra, role)));

        // create ResourceMappings
        return multimap.asMap().entrySet().stream().map(entry -> buildResourceMapping(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

    }

    private ResourceMapping buildResourceMapping(ResourcesAccess pRa, Collection<Role> pRoles) {
        ResourceMapping mapping = new ResourceMapping(
                ResourceAccessAdapter.createResourceAccess(pRa.getDescription(), null), pRa.getResource(),
                RequestMethod.valueOf(pRa.getVerb().toString()));
        mapping.setAutorizedRoles(pRoles.stream().map(role -> new RoleAuthority(role.getName()))
                .collect(Collectors.toList()));
        return mapping;
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities() {
        final List<RoleAuthority> results = new ArrayList<>();
        final Set<Role> roles = roleService.retrieveRoles();
        for (final Role role : roles) {
            final RoleAuthority roleAuth = new RoleAuthority(role.getName());
            boolean access = role.isCorsRequestsAuthorized();
            if (access && (role.getCorsRequestsAuthorizationEndDate() != null)) {
                access = LocalDateTime.now().isBefore(role.getCorsRequestsAuthorizationEndDate());
            }
            roleAuth.setAuthorizedIpAdresses(role.getAuthorizedAddresses());
            roleAuth.setCorsAccess(access);
            results.add(roleAuth);
        }
        return results;
    }

}
