package fr.cnes.regards.framework.security.endpoint;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 *
 * Class IAuthoritiesProvider
 *
 * Interface to define the method to get informations about Authorities resources and roles.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IAuthoritiesProvider {

    /**
     *
     * Register microservice given local endpoints to the administration service and retrieve configured endpoints.
     *
     * @param tenant
     *            working tenant
     * @param localEndpoints
     *            collected end points
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    List<ResourceMapping> registerEndpoints(String tenant, List<ResourceMapping> localEndpoints);

    /**
     *
     * Retrieve all roles authorities for specified tenant
     * 
     * @param tenant
     *            working tenant
     * @return all {@link RoleAuthority}
     * @since 1.0-SNAPSHOT
     */
    List<RoleAuthority> getRoleAuthorities(String tenant);
}
