/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

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
     * Role service
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Resources service
     */
    @Autowired
    private IResourcesService resourcesService;

    @Override
    public List<ResourceMapping> getResourcesAccessConfiguration() {
        final List<ResourceMapping> results = new ArrayList<>();
        final List<ResourcesAccess> resources = resourcesService.retrieveRessources();
        for (final ResourcesAccess resource : resources) {
            final ResourceMapping mapping = new ResourceMapping(resource.getResource(),
                    RequestMethod.valueOf(resource.getVerb().toString()));
            results.add(mapping);
        }
        return results;
    }

    @Override
    public List<String> getRoleAuthorizedAddress(final String pRole) throws SecurityException {
        try {
            final List<String> results = new ArrayList<>();
            final Role role = roleService.retrieveRole(pRole);
            results.addAll(role.getAuthorizedAddresses());
            return results;
        } catch (final EntityNotFoundException e) {
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
        } catch (final EntityNotFoundException e) {
            throw new SecurityException("Could not get CORS requests access", e);
        }
    }

}
