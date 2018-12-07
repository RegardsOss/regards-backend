package fr.cnes.regards.framework.security.endpoint;

import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 * Class IAuthoritiesProvider
 *
 * Interface to define the method to get informations about Authorities resources and roles.
 * @author CS
 */
public interface IAuthoritiesProvider {

    /**
     * Register microservice local endpoints to the administration service.
     * @param microserviceName related microservice
     * @param tenant working tenant
     * @param localEndpoints collected end points
     * @throws SecurityException if endpoints cannot be registered
     */
    void registerEndpoints(String microserviceName, String tenant, List<ResourceMapping> localEndpoints)
            throws SecurityException;

    /**
     * Retrieve all roles authorities for specified tenant and microservice
     * @param microserviceName related microservice
     * @param tenant working tenant
     * @return all {@link RoleAuthority}
     * @throws SecurityException if role cannot be retrieved
     */
    List<RoleAuthority> getRoleAuthorities(String microserviceName, String tenant) throws SecurityException;

    /**
     * Retrieve ResourcesAccesses from a given role on a given tenant and microservice and then build ResourceMappings
     * corresponding
     * @param microserviceName microservice from which we want the resources
     * @param tenant tenant on which we are working
     * @param roleName role considered
     * @return current ResourceMapping for a given role on a given microservice type for a given tenant
     */
    Set<ResourceMapping> getResourceMappings(String microserviceName, String tenant, String roleName);
}
