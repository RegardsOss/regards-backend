/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.microservices.core.security.configuration.MethodSecurityConfiguration;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

/**
 * REGARDS endpoint security voter to manage resource access dynamically at method level.
 *
 * @See {@link MethodSecurityConfiguration}
 * @author msordi
 *
 */
public class ResourceAccessVoter implements AccessDecisionVoter<Object> {

    private final MethodAutorizationService methodAuthService_;

    public ResourceAccessVoter(MethodAutorizationService pMethodAuthService) {
        this.methodAuthService_ = pMethodAuthService;
    }

    @Override
    public boolean supports(ConfigAttribute pAttribute) {
        return true;
    }

    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     *
     * @param pClazz
     *            the secure object
     *
     * @return always <code>true</code>
     */
    @Override
    public boolean supports(Class<?> pClazz) {
        return true;
    }

    @Override
    public int vote(Authentication pAuthentication, Object pObject, Collection<ConfigAttribute> pAttributes) {

        if (!(pObject instanceof MethodInvocation)) {
            return ACCESS_DENIED;
        }

        MethodInvocation mi = (MethodInvocation) pObject;
        ResourceAccess access = mi.getMethod().getAnnotation(ResourceAccess.class);
        RequestMapping mapping = mi.getMethod().getAnnotation(RequestMapping.class);
        RequestMapping classMapping = mi.getMethod().getDeclaringClass().getAnnotation(RequestMapping.class);
        Optional<List<GrantedAuthority>> option = methodAuthService_.getAuthorities(mapping, classMapping);
        Collection<? extends GrantedAuthority> userAuthorities = pAuthentication.getAuthorities();

        if ((access == null) || (userAuthorities == null) || !option.isPresent()) {
            return ACCESS_DENIED;
        }

        return checkAuthorities(option.get(), userAuthorities);
    }

    /**
     *
     * Check if the user authorities contains on of the method authorities
     *
     * @param pMethodAutorities
     *            list of granted authorities
     * @param pUserAutorities
     *            user authorities represented by a single role
     * @return granted or denied access (default)
     */
    private int checkAuthorities(List<GrantedAuthority> pMethodAutorities,
            Collection<? extends GrantedAuthority> pUserAutorities) {

        for (GrantedAuthority userAuthority : pUserAutorities) {
            for (GrantedAuthority resourceAuthority : pMethodAutorities) {
                if (userAuthority.getAuthority().equals(resourceAuthority.getAuthority())) {
                    return ACCESS_GRANTED;
                }
            }
        }
        return ACCESS_DENIED;
    }
}
