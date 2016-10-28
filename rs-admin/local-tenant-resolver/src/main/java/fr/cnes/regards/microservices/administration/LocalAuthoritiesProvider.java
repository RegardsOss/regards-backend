/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.IRoleService;

/**
 *
 * Class AuhtoritiesProvider
 *
 * Authorities provider for internal administration microservice access
 *
 * @author CS
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
    public List<String> getRoleAuthorizedAddress(final String pRole) {
        final List<String> results = new ArrayList<>();
        final Role role = roleService.retrieveRole(pRole);
        if (role != null) {
            results.addAll(role.getAuthorizedAddresses());
        }
        return results;
    }

    @Override
    public boolean hasCorsRequestsAccess(final String pRole) {
        boolean access = false;
        final Role role = roleService.retrieveRole(pRole);
        if (role != null) {
            access = role.isCorsRequestsAuthorized();
            if (access && (role.getCorsRequestsAuthorizationEndDate() != null)) {
                access = LocalDateTime.now().isBefore(role.getCorsRequestsAuthorizationEndDate());
            }
        }
        return access;
    }

}
