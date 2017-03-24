/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

/*
 * LICENSE_PLACEHOLDER
 */
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 *
 * Class DefaultAuthorityProvider
 *
 * Default Authorities provider. Provide default values for endpoints configuration and Roles.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class DefaultAuthorityProvider implements IAuthoritiesProvider {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorityProvider.class);

    /**
     * List of configurated authorities
     */
    @Value("${regards.security.authorities:#{null}}")
    private String[] authorities;

    /**
     * List of configurated roles
     */
    @Value("${regards.security.roles:#{null}}")
    private String[] roles;

    @Override
    public List<ResourceMapping> registerEndpoints(String tenant, final List<ResourceMapping> pLocalEndpoints) {
        LOG.warn("No Authority provider defined. Default one used."
                + " The local endpoints are not register to administration service. Only the default configuration is available");
        if (authorities != null) {
            LOG.debug("Initializing granted authorities from property file");
            for (final String auth : authorities) {
                final ResourceMapping resource = createResourceMapping(auth);
                if (resource != null) {
                    pLocalEndpoints.add(resource);
                }
            }
        }

        // Add default roles to returned endpoints
        pLocalEndpoints.forEach(endpoint -> {
            if ((endpoint != null) && (endpoint.getResourceAccess() != null)
                    && (endpoint.getResourceAccess().role() != null)) {
                endpoint.addAuthorizedRole(new RoleAuthority(endpoint.getResourceAccess().role().name()));
            }
        });
        return pLocalEndpoints;
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities(String tenant) {
        LOG.warn("No Authority provider defined. Only default roles are initialied.");
        final List<RoleAuthority> defaultRoleAuthorities = new ArrayList<>();
        for (final DefaultRole role : DefaultRole.values()) {
            defaultRoleAuthorities.add(new RoleAuthority(role.name()));
        }
        if (roles != null) {
            for (final String role : roles) {
                defaultRoleAuthorities.add(new RoleAuthority(role));
            }
        }
        return defaultRoleAuthorities;
    }

    /**
     *
     * createResourceMapping
     *
     * @param pAuthority
     *            authority
     * @return ResourceMapping
     * @since 1.0-SNAPSHOT
     */
    private ResourceMapping createResourceMapping(final String pAuthority) {
        ResourceMapping result = null;
        final String[] urlVerbRoles = pAuthority.split("\\|");
        if (urlVerbRoles.length > 1) {
            final String[] urlVerb = urlVerbRoles[0].split("@");
            if (urlVerb.length == 2) {
                // Url path
                final String url = urlVerb[0];
                // HTTP method
                final String verb = urlVerb[1];

                final RequestMethod httpVerb = RequestMethod.valueOf(verb);
                result = new ResourceMapping(url, null, httpVerb);

                // Roles
                for (int i = 1; i < urlVerbRoles.length; i++) {
                    result.addAuthorizedRole(new RoleAuthority(urlVerbRoles[i]));
                }
            }
        }
        return result;
    }

}
