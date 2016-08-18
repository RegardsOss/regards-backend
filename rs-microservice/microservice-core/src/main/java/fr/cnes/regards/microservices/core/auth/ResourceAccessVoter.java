package fr.cnes.regards.microservices.core.auth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;

public class ResourceAccessVoter implements AccessDecisionVoter<Object> {

    private final MethodAutorizationService authService;

    public ResourceAccessVoter(MethodAutorizationService authService) {
        this.authService = authService;
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     *
     * @param clazz
     *            the secure object
     *
     * @return always <code>true</code>
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {

        if (!(object instanceof MethodInvocation)) {
            return ACCESS_DENIED;
        }

        MethodInvocation mi = (MethodInvocation) object;
        ResourceAccess access = mi.getMethod().getAnnotation(ResourceAccess.class);
        RequestMapping mapping = mi.getMethod().getAnnotation(RequestMapping.class);
        RequestMapping classMapping = mi.getMethod().getDeclaringClass().getAnnotation(RequestMapping.class);
        Optional<List<GrantedAuthority>> option = authService.getAuthorities(mapping, classMapping);
        Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();

        if ((access == null) || (userAuthorities == null) || !option.isPresent()) {
            return ACCESS_DENIED;
        }

        return checkAuthorities(option.get(), userAuthorities);
    }

    /**
     *
     * Check if the user authorities contains on of the method authorities
     *
     * @param methodAutorities
     * @param userAutorities
     * @return
     * @since 0.0.1
     */
    private int checkAuthorities(List<GrantedAuthority> methodAutorities,
            Collection<? extends GrantedAuthority> userAutorities) {

        for (GrantedAuthority userAuthority : userAutorities) {
            for (GrantedAuthority resourceAuthority : methodAutorities) {
                if (userAuthority.getAuthority().equals(resourceAuthority.getAuthority())) {
                    return ACCESS_GRANTED;
                }
            }
        }

        return ACCESS_DENIED;
    }

}
