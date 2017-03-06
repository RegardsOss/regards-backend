/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContextException;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
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
 * @author Sylvain Vissiere-Guerinet
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
        // Register endpoints to administration service and retrieve configured ones
        final ResponseEntity<Void> response = resourcesClient.registerMicroserviceEndpoints(microserviceName,
                                                                                            pLocalEndpoints);
        if (response.getStatusCode().equals(HttpStatus.OK)) {

            // get a map that for each ResourcesAccess ra links the roles containing ra
            List<Role> roles = HateoasUtils.unwrapList(roleClient.retrieveRoleList().getBody());
            SetMultimap<ResourcesAccess, Role> multimap = HashMultimap.create();

            roles.forEach(role -> role.getPermissions().forEach(ra -> multimap.put(ra, role)));

            // create ResourceMappings
            return multimap.asMap().entrySet().stream()
                    .map(entry -> buildResourceMapping(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        } else {
            throw new ApplicationContextException("Error registring endpoints to administration service");
        }
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities() {
        final List<RoleAuthority> roleAuths = new ArrayList<>();

        final ResponseEntity<List<Resource<Role>>> result = roleClient.retrieveRoleList();

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

    private ResourceMapping buildResourceMapping(ResourcesAccess pRa, Collection<Role> pRoles) {
        ResourceMapping mapping = new ResourceMapping(
                ResourceAccessAdapter.createResourceAccess(pRa.getDescription(), null), pRa.getResource(),
                RequestMethod.valueOf(pRa.getVerb().toString()));
        mapping.setAutorizedRoles(pRoles.stream().map(role -> new RoleAuthority(role.getName()))
                .collect(Collectors.toList()));
        return mapping;
    }

}
