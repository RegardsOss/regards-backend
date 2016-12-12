/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContextException;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleDTO;

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
        final List<ResourceMapping> results = new ArrayList<>();
        // Register endpoints to administration service and retrieve configured ones
        final ResponseEntity<Void> response = resourcesClient.registerMicroserviceEndpoints(microserviceName,
                                                                                            pLocalEndpoints);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            // Then get configured endpoints

            final List<ResourcesAccess> resources = HateoasUtils.retrieveAllPages(100, (final Pageable pPageable) -> {
                return resourcesClient.retrieveResourcesAccesses(microserviceName, pPageable.getPageNumber(),
                                                                 pPageable.getPageSize());
            });
            resources.forEach(r -> results.add(r.toResourceMapping()));
        } else {
            throw new ApplicationContextException("Error registring endpoints to administration service");
        }
        return results;
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities() {
        final List<RoleAuthority> roleAuths = new ArrayList<>();

        final ResponseEntity<List<Resource<RoleDTO>>> result = roleClient.retrieveRoleList();

        if (result.getStatusCode().equals(HttpStatus.OK)) {
            final List<Resource<RoleDTO>> body = result.getBody();
            if (body != null) {
                final List<RoleDTO> roles = HateoasUtils.unwrapList(body);
                for (final RoleDTO role : roles) {
                    roleAuths.add(createRoleAuthority(role));
                }
            }
        }
        return roleAuths;
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
    private RoleAuthority createRoleAuthority(final RoleDTO pRole) {
        final RoleAuthority roleAuth = new RoleAuthority(pRole.getName());
        roleAuth.setAuthorizedIpAdresses(pRole.getAuthorizedAddresses());
        boolean access = pRole.isCorsRequestsAuthorized();
        if (access && (pRole.getCorsRequestsAuthorizationEndDate() != null)) {
            access = LocalDateTime.now().isBefore(pRole.getCorsRequestsAuthorizationEndDate());
        }
        roleAuth.setCorsAccess(access);
        return roleAuth;
    }

}
