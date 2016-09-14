/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.auth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Service MethodAutorizationServiceImpl Allow to set/get the REST resource method access authorizations. An
 * authorization is a defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author CS SI
 *
 */
@Service
public class MethodAutorizationServiceImpl implements MethodAutorizationService {

    Map<String, List<GrantedAuthority>> grantedAuthoritiesByResource;

    public MethodAutorizationServiceImpl() {
        grantedAuthoritiesByResource = new HashMap<>();
    }

    /**
     * Add a resource authorization
     */
    @Override
    public void setAutorities(String resourceName, GrantedAuthority... authorities) {
        if ((resourceName != null) && (authorities != null)) {
            if (grantedAuthoritiesByResource.containsKey(resourceName)) {
                List<GrantedAuthority> newAuthorities = grantedAuthoritiesByResource.get(resourceName);
                newAuthorities.addAll(Arrays.asList(authorities));
                grantedAuthoritiesByResource.put(resourceName, newAuthorities);
            }
            else {
                grantedAuthoritiesByResource.put(resourceName,
                                                 Arrays.asList(authorities).stream().collect(Collectors.toList()));
            }
        }
    }

    /**
     * Get a resource authorizations
     */
    @Override
    public Optional<List<GrantedAuthority>> getAuthorities(final RequestMapping access, RequestMapping classMapping) {
        String resourceId = ResourceAccessUtils.getIdentifier(access, classMapping);
        return Optional.ofNullable(grantedAuthoritiesByResource.get(resourceId));
    }

    @Override
    public List<GrantedAuthority> getAutoritiesById(String pId) {
        return grantedAuthoritiesByResource.get(pId);
    }
}
