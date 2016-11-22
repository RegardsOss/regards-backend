package fr.cnes.regards.framework.security.endpoint;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;

/**
 *
 * Class IAuthoritiesProvider
 *
 * Interface to derfine the method to get informations about Authorities resources and roles.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IAuthoritiesProvider {

    /**
     *
     * Register microservice given local endpoints to the administration service and retrieve configured endpoints.
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    List<ResourceMapping> registerEndpoints(List<ResourceMapping> pLocalEndpoints);

    /**
     *
     * Retrieve all the authorized address (IP) for the given Role
     *
     * @param pRole
     *            role
     * @return List<String>
     * @throws SecurityException
     *             when no Role of name <code>pRole</code> could be found
     * @since 1.0-SNAPSHOT
     */
    List<String> getRoleAuthorizedAddress(String pRole) throws SecurityException;

    /**
     *
     * Dertermine if the given role authority can use CORS requests
     *
     * @param pAuthority
     *            User role name
     * @return [true|false]
     * @throws SecurityException
     *             when no Role of name <code>pAutority</code> could be found
     * @since 1.0-SNAPSHOT
     */
    boolean hasCorsRequestsAccess(String pAuthority) throws SecurityException;
}
