/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
        final List<ResourceMapping> results = new ArrayList<>();
        final List<ResourcesAccess> resources = resourcesService.registerResources(pLocalEndpoints, microserviceName);
        resources.forEach(r -> results.add(r.toResourceMapping()));
        return results;
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities() {
        final List<RoleAuthority> results = new ArrayList<>();
        final List<Role> roles = roleService.retrieveRoleList();
        for (final Role role : roles) {
            final RoleAuthority roleAuth = new RoleAuthority(role.getName());
            roleAuth.setAuthorizedIpAdresses(role.getAuthorizedAddresses());
            roleAuth.setCorsAccess(role.isCorsRequestsAuthorized());
            results.add(roleAuth);
        }
        return results;
    }

    @Override
    public boolean hasCorsRequestsAccess(final String pRole) throws SecurityException {
        try {
            if (!RoleAuthority.isSysRole(pRole) && !RoleAuthority.isInstanceAdminRole(pRole)) {
                final Role role = roleService.retrieveRole(RoleAuthority.getRoleName(pRole));
                boolean access = role.isCorsRequestsAuthorized();
                if (access && (role.getCorsRequestsAuthorizationEndDate() != null)) {
                    access = LocalDateTime.now().isBefore(role.getCorsRequestsAuthorizationEndDate());
                }
                return access;
            } else {
                return true;
            }
        } catch (final EntityNotFoundException e) {
            throw new SecurityException("Could not get CORS requests access", e);
        }
    }

}
