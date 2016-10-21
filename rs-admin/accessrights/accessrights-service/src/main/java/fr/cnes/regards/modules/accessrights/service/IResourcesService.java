/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 * Class IResourcesService
 *
 * Business service to handle Resources
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IResourcesService {

    /**
     *
     * Collect all the resources from all the cloud connected microservices. The results is persisted in database and
     * returned.
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    List<ResourceMapping> collectResources();

    /**
     *
     * Retrieve all resources in database
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> retrieveRessources();

}
