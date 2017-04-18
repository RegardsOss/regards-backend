/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
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
     * @param pMicroserviceName
     *            microservice
     * @param pPageable
     *            the paging information
     *
     * @return {@link Page} of {@link ResourcesAccess}
     * @throws ModuleException
     *             if error occurs!
     */
    Page<ResourcesAccess> retrieveRessources(String pMicroserviceName, Pageable pPageable) throws ModuleException;

    /**
     *
     * Retrieve a {@link ResourcesAccess}
     *
     * @param pResourceId
     *            the if of the {@link ResourcesAccess} to retrieve
     *
     * @return {@link ResourcesAccess}
     * @throws ModuleException
     *             if error occurs!
     */
    ResourcesAccess retrieveRessource(Long pResourceId) throws ModuleException;

    /**
     * Update the given resource {@link ResourcesAccess}
     *
     * @param pResourceToUpdate
     *            {@link ResourcesAccess} to update
     *
     * @return {@link ResourcesAccess}
     * @throws ModuleException
     *             if error occurs!
     */
    ResourcesAccess updateResource(ResourcesAccess pResourceToUpdate) throws ModuleException;

    /**
     *
     * Merge the given resources for the given microservice to the already configured ones. If the resource does not
     * exists, the default role given is configured for resources access.
     *
     * @param pResourcesToRegister
     *            list of {@link ResourceMapping} to register
     * @param pMicroserviceName
     *            microservice owner of the resources to register
     * @throws ModuleException
     *             if error occurs!
     */
    void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName)
            throws ModuleException;

    /**
     * remove a resource access from a role and its descendants
     *
     * @param pRoleId
     *            role identifier
     * @param pResourcesAccessId
     *            resource identifier
     * @throws ModuleException
     *             if error occurs!
     */
    void removeRoleResourcesAccess(String pRoleName, Long pResourcesAccessId) throws ModuleException;

    /**
     *
     * Retrieve all resources for the given microservice and the given controller name
     *
     * @param pMicroserviceName
     *            microservice name
     * @param pControllerName
     *            controller name
     * @return @return List of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> retrieveMicroserviceControllerEndpoints(String pMicroserviceName, String pControllerName);

    /**
     *
     * Retreive microservice controllers names.
     *
     * @param pMicroserviceName
     *            microservice name
     * @return Array of String (controllers names)
     * @since 1.0-SNAPSHOT
     */
    List<String> retrieveMicroserviceControllers(String pMicroserviceName);

}
