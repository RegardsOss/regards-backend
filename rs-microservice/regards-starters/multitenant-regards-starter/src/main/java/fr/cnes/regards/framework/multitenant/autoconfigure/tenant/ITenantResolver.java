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
@FunctionalInterface
public interface ITenantResolver {

    Set<String> getAllTenants();
}
