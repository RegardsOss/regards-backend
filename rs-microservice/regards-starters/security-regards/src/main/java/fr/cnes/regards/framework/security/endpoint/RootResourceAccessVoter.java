/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.Collection;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * Class RootResourceAccessVoter
 *
 * REGARDS endpoint security voter to manage resource access for specific user with role root_admin.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class RootResourceAccessVoter implements AccessDecisionVoter<Object> {

    /**
     * Always access granted user role.
     */
    public static final String ROOT_ADMIN_AUHTORITY = "ROOT_ADMIN";

    @Override
    public boolean supports(final ConfigAttribute pArg0) {
        return true;
    }

    @Override
    public boolean supports(final Class<?> pArg0) {
        return true;
    }

    @Override
    public int vote(final Authentication pAuthentication, final Object pObject,
            final Collection<ConfigAttribute> pAttributes) {
        // Default behavior : deny access
        int access = ACCESS_DENIED;

        // If authentication do not contains authority, deny access
        if ((pAuthentication.getAuthorities() != null) && !pAuthentication.getAuthorities().isEmpty()) {
            for (final GrantedAuthority auth : pAuthentication.getAuthorities()) {
                if (auth.getAuthority().equals("ROLE_" + ROOT_ADMIN_AUHTORITY)) {
                    access = ACCESS_GRANTED;
                    break;
                }
            }
        }

        return access;
    }

}
