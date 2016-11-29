/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.JwtTokenUtils;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
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
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MicroserviceAuthoritiesProvider.class);

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * Security service
     */
    private final JWTService jwtService;

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
            final IRolesClient pRolesClient, final JWTService pJwtService) {
        super();
        microserviceName = pMicroserviceName;
        resourcesClient = pResourcesclient;
        roleClient = pRolesClient;
        jwtService = pJwtService;
    }

    @Override
    public List<ResourceMapping> registerEndpoints(final List<ResourceMapping> pLocalEndpoints) {
        // Register endpoints to administration service and retrieve configured ones
        final ResponseEntity<List<ResourceMapping>> response = resourcesClient
                .registerMicroserviceEndpoints(microserviceName, pLocalEndpoints);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return response.getBody();
        } else {
            throw new ApplicationContextException("Error registring endpoints to administration service");
        }
    }

    @Override
    public List<String> getRoleAuthorizedAddress(final String pRole) throws SecurityException {
        final List<String> addresses = new ArrayList<>();

        // Execute role request with Sys role
        final Function<String, ResponseEntity<Resource<Role>>> retreiveRole = JwtTokenUtils
                .asSafeCallableOnRole(() -> roleClient.retrieveRole(pRole), jwtService);

        final ResponseEntity<Resource<Role>> result = retreiveRole.apply(RoleAuthority.getSysRole(microserviceName));

        if (result.getStatusCode().equals(HttpStatus.OK)) {
            final Resource<Role> body = result.getBody();
            if ((body != null) && (body.getContent() != null)) {
                addresses.addAll(body.getContent().getAuthorizedAddresses());
            }
        }
        return addresses;
    }

    @Override
    public boolean hasCorsRequestsAccess(final String pRole) throws SecurityException {
        boolean access = false;
        if (!RoleAuthority.isSysRole(pRole) && !(RoleAuthority.isInstanceAdminRole(pRole))) {

            // Execute role request with Sys role
            final Function<String, ResponseEntity<Resource<Role>>> retreiveRole = JwtTokenUtils
                    .asSafeCallableOnRole(() -> roleClient.retrieveRole(RoleAuthority.getRoleName(pRole)), jwtService);

            final ResponseEntity<Resource<Role>> result = retreiveRole
                    .apply(RoleAuthority.getSysRole(microserviceName));

            if (result.getStatusCode().equals(HttpStatus.OK) && (result.getBody() != null)
                    && (result.getBody().getContent() != null)) {
                access = result.getBody().getContent().isCorsRequestsAuthorized();
            }
            return access;
        } else {
            return true;
        }
    }

}
