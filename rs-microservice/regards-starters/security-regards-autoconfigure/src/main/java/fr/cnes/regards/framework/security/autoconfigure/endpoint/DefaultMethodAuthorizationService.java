/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 * Service MethodAutorizationServiceImpl<br/>
 * Allow to set/get the REST resource method access authorizations.<br/>
 * An authorization is defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author CS SI
 *
 */
public class DefaultMethodAuthorizationService implements IMethodAuthorizationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMethodAuthorizationService.class);

    /**
     * List of configurated authorities
     */
    @Value("${regards.security.authorities:#{null}}")
    private String[] authorities;

    /**
     * Authorities cache that provide granted authorities per resource
     */
    private final Map<String, ArrayList<GrantedAuthority>> grantedAuthoritiesByResource;

    public DefaultMethodAuthorizationService() {
        grantedAuthoritiesByResource = new HashMap<>();
    }

    /**
     * After bean contruction, read configurated authorities
     */
    @PostConstruct
    public void init() {
        if (authorities != null) {
            LOG.debug("Initializing granted authorities from property file");
            for (String auth : authorities) {
                final String[] urlVerbRoles = auth.split("\\|");
                if (urlVerbRoles.length > 1) {
                    final String[] urlVerb = urlVerbRoles[0].split("@");
                    if (urlVerb.length == 2) {
                        // Url path
                        final String url = urlVerb[0];
                        // HTTP method
                        final String verb = urlVerb[1];

                        try {
                            final RequestMethod httpVerb = RequestMethod.valueOf(verb);

                            // Roles
                            final String[] roles = new String[urlVerbRoles.length - 1];
                            for (int i = 1; i < urlVerbRoles.length; i++) {
                                roles[i - 1] = urlVerbRoles[i];
                            }

                            setAuthorities(url, httpVerb, roles);
                        } catch (IllegalArgumentException pIAE) {
                            LOG.error("Cannot retrieve HTTP method from {}", verb);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add a resource authorization
     */
    @Override
    public void setAuthorities(ResourceMapping pResourceMapping, GrantedAuthority... pAuthorities) {
        if ((pResourceMapping != null) && (pAuthorities != null)) {
            final String resourceId = pResourceMapping.getResourceMappingId();
            final ArrayList<GrantedAuthority> newAuthorities;
            if (grantedAuthoritiesByResource.containsKey(resourceId)) {
                final Set<GrantedAuthority> set = new LinkedHashSet<>(grantedAuthoritiesByResource.get(resourceId));
                for (GrantedAuthority grant : pAuthorities) {
                    set.add(grant);
                }
                newAuthorities = new ArrayList<>(set);
            } else {
                newAuthorities = new ArrayList<>();
                newAuthorities.addAll(Arrays.asList(pAuthorities));
            }
            grantedAuthoritiesByResource.put(resourceId, newAuthorities);
        }
    }

    @Override
    public Optional<List<GrantedAuthority>> getAuthorities(final ResourceMapping pResourceMapping) {
        return Optional.ofNullable(grantedAuthoritiesByResource.get(pResourceMapping.getResourceMappingId()));
    }

    @Override
    public void setAuthorities(String pUrlPath, RequestMethod pMethod, String... pRoleNames) {
        // Validate
        Assert.notNull(pUrlPath, "Path to resource cannot be null.");
        Assert.notNull(pMethod, "HTTP method cannot be null.");
        Assert.notNull(pRoleNames, "At least one role is required.");

        // Build granted authorities
        final List<GrantedAuthority> newAuthorities = new ArrayList<>();
        for (String role : pRoleNames) {
            newAuthorities.add(new RoleAuthority(role));
        }

        setAuthorities(new ResourceMapping(Optional.of(pUrlPath), pMethod),
                       newAuthorities.toArray(new GrantedAuthority[0]));
    }
}
