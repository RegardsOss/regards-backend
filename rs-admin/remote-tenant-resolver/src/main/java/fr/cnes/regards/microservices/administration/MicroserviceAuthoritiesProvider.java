/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
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
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceAuthoritiesProvider implements IAuthoritiesProvider {

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * Administration microservice REST client
     */
    private final IResourcesClient resourcesClient;

    /**
     * Administration microservice REST client
     */
    private final IRolesClient roleClient;

    /**
     *
     * Constructor
     *
     * @param pRoleClient
     *            Feign client to query administration service for roles
     * @param pResourcesClient
     *            Feign client to query administration service for resources
     * @since 1.0-SNAPSHOT
     */
    public MicroserviceAuthoritiesProvider(final String pMicroserviceName, final IResourcesClient pResourcesclient,
            final IRolesClient pRolesClient) {
        super();
        microserviceName = pMicroserviceName;
        resourcesClient = pResourcesclient;
        roleClient = pRolesClient;
    }

    @Override
    public List<ResourceMapping> registerEndpoints(final List<ResourceMapping> pLocalEndpoints) {
        final List<ResourceMapping> resourcesMapping = new ArrayList<>();
        // Register endpoints to administration service and retrieve configured ones
        final ResponseEntity<List<Resource<ResourcesAccess>>> response = resourcesClient
                .registerMicroserviceEndpoints(microserviceName, pLocalEndpoints);
        if (response.getStatusCode().equals(HttpStatus.OK) && (response.getBody() != null)) {
            final List<ResourcesAccess> configuredResources = HateoasUtils.unwrapList(response.getBody());
            configuredResources.forEach(r -> resourcesMapping.add(r.toResourceMapping()));
        }
        return resourcesMapping;
    }

    @Override
    public List<String> getRoleAuthorizedAddress(final String pRole) throws SecurityException {
        try {
            final List<String> addresses = new ArrayList<>();
            final ResponseEntity<Resource<Role>> result = roleClient.retrieveRole(pRole);
            if (result.getStatusCode().equals(HttpStatus.OK)) {
                final Resource<Role> body = result.getBody();
                if (body != null) {
                    addresses.addAll(body.getContent().getAuthorizedAddresses());
                }
            }
            return addresses;
        } catch (final EntityNotFoundException e) {
            throw new SecurityException("Could not get role authorized addesses", e);
        }
    }

    @Override
    public boolean hasCorsRequestsAccess(final String pRole) throws SecurityException {
        try {
            boolean access = false;
            if (!RoleAuthority.isSysRole(pRole)) {
                final ResponseEntity<Resource<Role>> result = roleClient.retrieveRole(RoleAuthority.getRoleName(pRole));
                if (result.getStatusCode().equals(HttpStatus.OK)) {
                    access = result.getBody().getContent().isCorsRequestsAuthorized();
                }
                return access;
            } else {
                return true;
            }
        } catch (final EntityNotFoundException e) {
            throw new SecurityException("Could not get cors requests access", e);
        }
    }

}
