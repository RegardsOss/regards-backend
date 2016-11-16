/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.autoconfigure.tenant;

import java.util.Set;

/**
 * Interface to retrieve tenant information.
 *
 * @author msordi
 *
 */
public interface ITenantResolver {

    Set<String> getAllTenants();
}
