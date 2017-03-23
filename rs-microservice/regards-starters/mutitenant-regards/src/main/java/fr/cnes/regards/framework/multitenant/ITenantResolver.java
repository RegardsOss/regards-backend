/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant;

import java.util.Set;

/**
 * Interface to retrieve tenant information.
 *
 * @author msordi
 *
 */
public interface ITenantResolver {

    /**
     * @return all tenants regardless its configuration
     */
    Set<String> getAllTenants();

    /**
     *
     * @return all tenants fully configured
     */
    Set<String> getAllActiveTenants();
}
