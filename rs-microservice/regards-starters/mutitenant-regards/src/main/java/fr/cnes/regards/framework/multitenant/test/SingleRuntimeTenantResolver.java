/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.test;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Single tenant resolver. Useful for testing purpose.
 *
 * @author Marc Sordi
 *
 */
public class SingleRuntimeTenantResolver implements IRuntimeTenantResolver {

    /**
     * Unique tenant
     */
    private String tenant;

    public SingleRuntimeTenantResolver(String pTenant) {
        this.tenant = pTenant;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public void forceTenant(String pTenant) {
        this.tenant = pTenant;
    }

}
