package fr.cnes.regards.framework.security.endpoint;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

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

    /**
     *
     * Retrieve all roles authorities
     *
     * @return all {@link RoleAuthority}
     * @since 1.0-SNAPSHOT
     */
    List<RoleAuthority> getRoleAuthorities();
}
