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
    void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName);

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
    void removeRoleResourcesAccess(Long pRoleId, Long pResourcesAccessId) throws ModuleException;

}
