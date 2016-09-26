/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.security.utils.endpoint.RoleAuthority;

/**
 * Service MethodAutorizationServiceImpl<br/>
 * Allow to set/get the REST resource method access authorizations.<br/>
 * An authorization is defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author CS SI
 *
 */
@Service
public class MethodAutorizationServiceImpl implements MethodAutorizationService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodAutorizationServiceImpl.class);

    @Value("${regards.security.authorities:#{null}}")
    private String[] authorities_;

    Map<String, List<GrantedAuthority>> grantedAuthoritiesByResource_;

    public MethodAutorizationServiceImpl() {
        grantedAuthoritiesByResource_ = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        if (authorities_ != null) {
            LOG.debug("Initializing granted authorities from property file");
            for (String auth : authorities_) {
                String[] urlVerbRoles = auth.split("\\|");
                if (urlVerbRoles.length > 1) {
                    String[] urlVerb = urlVerbRoles[0].split("@");
                    if (urlVerb.length == 2) {
                        // Url path
                        String url = urlVerb[0];
                        // HTTP method
                        String verb = urlVerb[1];
                        RequestMethod httpVerb;
                        try {
                            httpVerb = RequestMethod.valueOf(verb);

                            // Roles
                            String[] roles = new String[urlVerbRoles.length - 1];
                            for (int i = 1; i < urlVerbRoles.length; i++) {
                                roles[i - 1] = urlVerbRoles[i];
                            }

                            setAuthorities(url, httpVerb, roles);
                        }
                        catch (IllegalArgumentException pIAE) {
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
            String resourceId = pResourceMapping.getResourceMappingId();
            if (grantedAuthoritiesByResource_.containsKey(resourceId)) {
                List<GrantedAuthority> newAuthorities = grantedAuthoritiesByResource_.get(resourceId);
                newAuthorities.addAll(Arrays.asList(pAuthorities));
                grantedAuthoritiesByResource_.put(resourceId, newAuthorities);
            }
            else {
                grantedAuthoritiesByResource_.put(resourceId, Arrays.asList(pAuthorities));
            }
        }
    }

    @Override
    public Optional<List<GrantedAuthority>> getAuthorities(final ResourceMapping pResourceMapping) {
        return Optional.ofNullable(grantedAuthoritiesByResource_.get(pResourceMapping.getResourceMappingId()));
    }

    @Override
    public void setAuthorities(String pUrlPath, RequestMethod pMethod, String... pRoleNames) {
        // Validate
        Assert.notNull(pUrlPath, "Path to resource cannot be null.");
        Assert.notNull(pMethod, "HTTP method cannot be null.");
        Assert.notNull(pRoleNames, "At least one role is required.");

        // Build granted authorities
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : pRoleNames) {
            authorities.add(new RoleAuthority(role));
        }

        setAuthorities(new ResourceMapping(Optional.empty(), Optional.of(pUrlPath), pMethod),
                       authorities.toArray(new GrantedAuthority[0]));
    }
}
