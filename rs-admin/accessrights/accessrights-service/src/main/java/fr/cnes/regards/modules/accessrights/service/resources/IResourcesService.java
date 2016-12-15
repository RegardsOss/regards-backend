/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
     * Retrieve paged resources in database
     *
     * @param pPageable
     *            the paging information
     *
     * @return {@link Page} of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    Page<ResourcesAccess> retrieveRessources(Pageable pPageable);

    /**
     *
     * Retrieve all resources in database
     *
     * @return List of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> retrieveRessources();

    /**
     *
     * Retrieve a {@link ResourcesAccess}
     * 
     * @param pResourceId
     *            the if of the {@link ResourcesAccess} to retrieve
     *
     * @return {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    ResourcesAccess retrieveRessource(Long pResourceId) throws EntityNotFoundException;

    /**
     * Update the given resource {@link ResourcesAccess}
     *
     * @param pResourceToUpdate
     *            {@link ResourcesAccess} to update
     *
     * @return {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    ResourcesAccess updateResource(ResourcesAccess pResourceToUpdate) throws EntityNotFoundException;

    /**
     *
     * Retrieve all resources in database for the given microservice
     *
     * @param pMicroserviceName
     *            the microservice name
     * @param pPageable
     *            the paging information
     * @return List of {@link ResourcesAccess}
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    Page<ResourcesAccess> retrieveMicroserviceRessources(String pMicroserviceName, final Pageable pPageable);

    /**
     *
     * Merge the given resources for the given microservice to the already configured ones. If the resource does not
     * exists, the default role given is configured for resources access.
     *
     * @param pResourcesToRegister
     *            list of {@link ResourceMapping} to register
     * @param pMicroserviceName
     *            microservice owner of the resources to register
     * @since 1.0-SNAPSHOT
     */
    void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName);

}
