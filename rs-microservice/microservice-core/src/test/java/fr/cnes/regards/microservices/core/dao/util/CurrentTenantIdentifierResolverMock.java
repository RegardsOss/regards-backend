/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.util;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantIdentifierResolverMock implements CurrentTenantIdentifierResolver {

    private String tenantId_ = "default";

    @Override
    public String resolveCurrentTenantIdentifier() {
        return this.tenantId_;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    public void setTenant(String pTenant) {
        this.tenantId_ = pTenant;
    }
}
