/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
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
    @Value("${spring.application.name")
    private String microserviceName;

    /**
     * Role service
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Resources service
     */
    @Autowired
    private IResourcesService resourcesService;

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
        final List<ResourcesAccess> resources = resourcesService.retrieveRessources();

        List<ResourcesAccess> newResources = new ArrayList<>();
        // Create missing resources from local endpoints
        for (final ResourceMapping resource : pLocalEndpoints) {
            boolean isConfigured = false;
            for (final ResourcesAccess configuredResource : resources) {
                if (resource.getFullPath().equals(configuredResource.getResource())
                        && resource.getMethod().toString().equals(configuredResource.getVerb().toString())) {
                    isConfigured = true;
                    break;
                }
            }
            if (!isConfigured) {
                newResources.add(new ResourcesAccess(resource, microserviceName));
            }
        }
        newResources = resourcesService.saveResources(newResources);
        newResources.forEach(r -> results.add(r.toResourceMapping()));

        return results;
    }

    @Override
    public List<String> getRoleAuthorizedAddress(final String pRole) throws SecurityException {
        try {
            final List<String> results = new ArrayList<>();
            final Role role = roleService.retrieveRole(pRole);
            results.addAll(role.getAuthorizedAddresses());
            return results;
        } catch (final ModuleEntityNotFoundException e) {
            throw new SecurityException("Could not get role authorized addresses", e);
        }
    }

    @Override
    public boolean hasCorsRequestsAccess(final String pRole) throws SecurityException {
        try {
            if (!RoleAuthority.isSysRole(pRole)) {
                final Role role = roleService.retrieveRole(RoleAuthority.getRoleName(pRole));
                boolean access = role.isCorsRequestsAuthorized();
                if (access && (role.getCorsRequestsAuthorizationEndDate() != null)) {
                    access = LocalDateTime.now().isBefore(role.getCorsRequestsAuthorizationEndDate());
                }
                return access;
            } else {
                return true;
            }
        } catch (final ModuleEntityNotFoundException e) {
            throw new SecurityException("Could not get CORS requests access", e);
        }
    }

}
