/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.util;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 *
 * Class CurrentTenantIdentifierResolverMock
 *
 * Mock to overload the tenant resolver. This mock allow to manually set the tenant instead of reading it from the JWT
 * Token in the SecurityContext
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class CurrentTenantIdentifierResolverMock implements CurrentTenantIdentifierResolver {

    /**
     * Default tenant to user
     */
    private String tenantId = "default";

    @Override
    public String resolveCurrentTenantIdentifier() {
        return this.tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    /**
     *
     * Setter
     *
     * @param pTenant
     *            Tenant or project to use
     * @since 1.0-SNAPSHOT
     */
    public void setTenant(String pTenant) {
        this.tenantId = pTenant;
    }
}
