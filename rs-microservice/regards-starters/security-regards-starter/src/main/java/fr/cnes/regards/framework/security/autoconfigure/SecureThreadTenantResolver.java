/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.multitenant.IThreadTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Retrieve thread tenant according to security context
 *
 * @author Marc Sordi
 *
 */
public class SecureThreadTenantResolver implements IThreadTenantResolver {

    @Override
    public String getTenant() {
        final JWTAuthentication authentication = ((JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication());
        if (authentication != null) {
            return authentication.getTenant();
        } else {
            return null;
        }
    }
}
