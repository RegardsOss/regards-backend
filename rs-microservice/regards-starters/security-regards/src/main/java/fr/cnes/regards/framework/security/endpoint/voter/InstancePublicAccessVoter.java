/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint.voter;

import java.util.Collection;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.IInstancePublicAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * This class authorizes access to all endpoints for instance administrator.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
public class InstancePublicAccessVoter implements IInstancePublicAccessVoter {

    /**
     * Instance tenant name
     */
    private final String instanceTenantName;

    /**
     * Constructor
     *
     * @param pInstanceTenantName
     *            instance
     */
    public InstancePublicAccessVoter(final String pInstanceTenantName) {
        this.instanceTenantName = pInstanceTenantName;
    }

    @Override
    public int vote(final Authentication pAuthentication, final Object pObject,
            final Collection<ConfigAttribute> pAttributes) {
        // Default behavior : deny access
        int access = ACCESS_DENIED;

        // Get authorized ip associated to given role
        final JWTAuthentication authentication = (JWTAuthentication) pAuthentication;

        if (pObject instanceof MethodInvocation) {

            final MethodInvocation mi = (MethodInvocation) pObject;
            // Get resource access anotation
            final ResourceAccess[] ra = mi.getMethod().getAnnotationsByType(ResourceAccess.class);
            if ((ra.length > 0) && (ra[0].role() != null)) {
                // If the resource access default ROLE is PUBLIC and the user role is PUBLIC and the tenant is instance,
                // allow access.
                if (ra[0].role().name().equals(DefaultRole.PUBLIC.toString())) {
                    // If authenticated user is the instance admin user allow all.
                    @SuppressWarnings("unchecked")
                    final List<RoleAuthority> roles = (List<RoleAuthority>) authentication.getAuthorities();
                    if ((authentication.getTenant().equals(instanceTenantName)) && roles.get(0).getAuthority()
                            .equals(RoleAuthority.getRoleAuthority(DefaultRole.PUBLIC.toString()))) {
                        access = ACCESS_GRANTED;
                    }
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
