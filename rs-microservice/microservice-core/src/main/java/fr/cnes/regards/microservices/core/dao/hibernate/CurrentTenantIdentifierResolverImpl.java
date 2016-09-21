/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.hibernate;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 *
 * Spring component used by Hibernante to determine the tenant to use during the datasource connection creation.
 *
 *
 * @author CS
 * @since 1.0-SNAPSHOT.
 */
@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    private final String DEFAULT_TENANT = "default";

    private String tenant = DEFAULT_TENANT;

    @Override
    public String resolveCurrentTenantIdentifier() {
        // TODO : Get tenant from request JWT Token
        return this.tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    public void setTenant(String pTenant) {
        this.tenant = pTenant;
    }
}
