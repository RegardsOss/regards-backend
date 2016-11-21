/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Spring component used by Hibernante to determine the tenant to use during the datasource connection creation.
 *
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT.
 */
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    /**
     * Default tenant
     */
    private static final String DEFAULT_TENANT = "default";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = DEFAULT_TENANT;
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            tenant = authentication.getTenant();
        }
        return tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
