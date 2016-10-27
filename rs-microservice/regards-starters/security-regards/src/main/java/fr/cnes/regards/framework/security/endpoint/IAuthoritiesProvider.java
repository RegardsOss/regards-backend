package fr.cnes.regards.framework.security.endpoint;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;

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
     * Retrieve all the resourcesAccesses configuration for the current microservice
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    List<ResourceMapping> getResourcesAccessConfiguration();

    /**
     *
     * Retrieve all the authorized address (IP) for the given Role
     *
     * @param pRole
     *            role
     * @return List<String>
     * @since 1.0-SNAPSHOT
     */
    List<String> getRoleAuthorizedAddress(String pRole);

    /**
     *
     * Dertermine if the given role authority can use CORS requests
     *
     * @param pAuthority
     *            User role name
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    boolean hasCorsRequestsAccess(String pAuthority);
}
