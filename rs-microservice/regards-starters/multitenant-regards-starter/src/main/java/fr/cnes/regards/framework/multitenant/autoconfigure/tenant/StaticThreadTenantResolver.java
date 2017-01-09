/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.autoconfigure.tenant;

import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.multitenant.IThreadTenantResolver;

/**
 *
 * This implementation is intended to be used for development purpose.<br/>
 * In production, an on request dynamic resolver must be set to retrieve request tenant.
 *
 * @author Marc Sordi
 *
 */
public class StaticThreadTenantResolver implements IThreadTenantResolver {

    /**
     * Static tenant
     */
    @Value("${regards.tenant:#{null}}")
    private String tenant;

    @Override
    public String getTenant() {
        return tenant;
    }

}
