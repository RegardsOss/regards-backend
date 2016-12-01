/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
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
    public List<RoleAuthority> getRoleAuthorities() {
        final List<RoleAuthority> roleAuths = new ArrayList<>();

        // Execute role request with Sys role
        final Function<String, ResponseEntity<List<Resource<Role>>>> retreiveRole = JwtTokenUtils
                .asSafeCallableOnRole(() -> roleClient.retrieveRoleList(), jwtService);

        final ResponseEntity<List<Resource<Role>>> result = retreiveRole
                .apply(RoleAuthority.getSysRole(microserviceName));

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
        boolean access = pRole.isCorsRequestsAuthorized();
        if (access && (pRole.getCorsRequestsAuthorizationEndDate() != null)) {
            access = LocalDateTime.now().isBefore(pRole.getCorsRequestsAuthorizationEndDate());
        }
        roleAuth.setCorsAccess(access);
        return roleAuth;
    }

}
