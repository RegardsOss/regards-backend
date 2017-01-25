/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

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

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public CurrentTenantIdentifierResolverImpl(IRuntimeTenantResolver pThreadTenantResolver) {
        this.runtimeTenantResolver = pThreadTenantResolver;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        return runtimeTenantResolver.getTenant() == null ? DEFAULT_TENANT : runtimeTenantResolver.getTenant();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
