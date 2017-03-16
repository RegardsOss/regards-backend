/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant;

/**
 * In a request context, this resolver allows to retrieve request tenant.
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
     * Force runtime tenant to a specific value
     *
     * @param pTenant tenant
     */
    void forceTenant(String pTenant);
}
