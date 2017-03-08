/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint.voter;

import java.util.Collection;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import fr.cnes.regards.framework.security.utils.endpoint.ISystemAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 *
 * This class authorizes access to all endpoints for system internal call between microservices.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
public class SystemAccessVoter implements ISystemAccessVoter {

    @Override
    public int vote(final Authentication pAuthentication, final Object pObject,
            final Collection<ConfigAttribute> pAttributes) {
        // Default behavior : deny access
        int access = ACCESS_DENIED;

        // If authentication do not contains authority, deny access
        if ((pAuthentication.getAuthorities() != null) && !pAuthentication.getAuthorities().isEmpty()) {
            for (final GrantedAuthority auth : pAuthentication.getAuthorities()) {
                if (RoleAuthority.isSysRole(auth.getAuthority())) {
                    access = ACCESS_GRANTED;
                    break;
                }
            }
        }

        return access;
    }

    @Override
    public boolean supports(final ConfigAttribute pArg0) {
        return true;
    }

    @Override
    public boolean supports(final Class<?> pArg0) {
        return true;
    }

}
