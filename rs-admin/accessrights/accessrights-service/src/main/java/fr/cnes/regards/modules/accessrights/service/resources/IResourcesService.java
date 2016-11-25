/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 * Class IResourcesService
 *
 * Business service to handle Resources
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IResourcesService {

    /**
     *
     * Retrieve all resources in database
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> retrieveRessources();

    /**
     *
     * Retrieve all resources in database for the given microservice
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> retrieveMicroserviceRessources(String pMicroserviceName);

    /**
     *
     * Merge the given resources for the given microservice to the already configured ones. If the resource does not
     * exists, the default role given is configured for resources access.
     *
     * @param pResourcesToRegister
     *            list of {@link ResourceMapping} to register
     * @param pMicroserviceName
     *            microservice owner of the resources to register
     * @return Configured Resources
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> registerResources(final List<ResourceMapping> pResourcesToRegister,
            final String pMicroserviceName);

}
