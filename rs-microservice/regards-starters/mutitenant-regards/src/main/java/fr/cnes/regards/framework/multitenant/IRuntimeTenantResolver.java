/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant;

/**
 * In a request context, this resolver allows to retrieve request tenant. This resolver must be thread safe.
 *
 * @author Marc Sordi
 *
 */
public interface IRuntimeTenantResolver {

    /**
     *
     * @return runtime tenant
     */
    String getTenant();

    /**
     *
     * Does the current tenant is instance
     *
     * @return true|false
     */
    Boolean isInstance();

    /**
     * Force runtime tenant to a specific value on current thread.<br/>
     * We recommend to use {@link IRuntimeTenantResolver#clearTenant()} to clean the thread in a finally clause.<br/>
     * It is mostly recommended for server threads as they are reused.
     *
     * @param pTenant
     *            tenant
     */
    void forceTenant(String pTenant);

    /**
     * Clear forced tenant on current thread
     */
    public void clearTenant();
}
