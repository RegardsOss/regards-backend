/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.test;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Single tenant resolver. Useful for testing purpose. Add multi-thread management.
 *
 * @author Marc Sordi
 * @author oroussel
 */
public class SingleRuntimeTenantResolver implements IRuntimeTenantResolver {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 3055171991646021858L;

    // Thread safe tenant holder for forced tenant
    private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

    public SingleRuntimeTenantResolver(final String pTenant) {
        tenantHolder.set(pTenant);
    }

    @Override
    public String getTenant() {
        return tenantHolder.get();
    }

    @Override
    public void forceTenant(final String pTenant) {
        tenantHolder.set(pTenant);
    }

    @Override
    public Boolean isInstance() {
        return Boolean.FALSE;
    }

}
