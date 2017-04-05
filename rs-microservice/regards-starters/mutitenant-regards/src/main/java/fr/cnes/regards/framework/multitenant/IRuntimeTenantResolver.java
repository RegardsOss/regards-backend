/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant;

import java.io.Serializable;

/**
 * In a request context, this resolver allows to retrieve request tenant.
 *
 * @author Marc Sordi
 *
 */
public interface IRuntimeTenantResolver extends Serializable {

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
