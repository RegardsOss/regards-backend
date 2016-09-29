/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Interface for method authorization access service
 *
 * @author CS SI
 *
 */
public interface MethodAuthorizationService {

    /**
     * Get all granted role to access a particular resource access
     *
     * @param pResourceMapping
     *            resource mapping configuration
     * @return list of granted authorities
     */
    Optional<List<GrantedAuthority>> getAuthorities(ResourceMapping pResourceMapping);

    /**
     * Register granted roles for a particular resource access
     *
     * @param pResourceMapping
     *            resource access
     * @param pAuthorities
     *            list of roles
     */
    public void setAuthorities(ResourceMapping pResourceMapping, GrantedAuthority... pAuthorities);

    /**
     * Register granted roles for a particular resource access.<br/>
     * Resource access is defined by :
     * <ul>
     * <li>URL path</li>
     * <li>HTTP method</li>
     * </ul>
     *
     * @param pUrlPath
     *            URL path to the resource (concatenation of class path and method path)
     * @param pMethod
     *            a {@link RequestMethod}
     * @param pRoleNames
     *            list of granted roles
     */
    public void setAuthorities(String pUrlPath, RequestMethod pMethod, String... pRoleNames);

}
