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
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 *
 * Class DefaultAuthorityProvider
 *
 * TODO description.
 *
 * @author CS
 * @since TODO
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

    @Override
    public List<ResourceMapping> getResourcesAccessConfiguration() {
        final List<ResourceMapping> resources = new ArrayList<>();
        if (authorities != null) {
            LOG.debug("Initializing granted authorities from property file");
            for (final String auth : authorities) {
                final ResourceMapping resource = createResourceMapping(auth);
                if (resource != null) {
                    resources.add(resource);
                }
            }
        }
        return resources;
    }

    @Override
    public List<String> getRoleAuthorizedAddress(final String pRole) {
        return new ArrayList<>();
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
                result = new ResourceMapping(null, url, httpVerb);

                // Roles
                for (int i = 1; i < urlVerbRoles.length; i++) {
                    result.addAuthorizedRole(new RoleAuthority(urlVerbRoles[i]));
                }
            }
        }
        return result;
    }

}
