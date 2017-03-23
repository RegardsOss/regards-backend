/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Spring component used by Hibernate to determine the tenant to use during the datasource connection creation.
 *
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT.
 */
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentTenantIdentifierResolverImpl.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public CurrentTenantIdentifierResolverImpl(IRuntimeTenantResolver pThreadTenantResolver) {
        this.runtimeTenantResolver = pThreadTenantResolver;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = runtimeTenantResolver.getTenant() != null ? runtimeTenantResolver.getTenant() : "default";
        LOGGER.debug("Resolved tenant : {}", tenant);
        return tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
