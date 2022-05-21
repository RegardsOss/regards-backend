/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.resources;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Class IResourcesService
 * <p>
 * Business service to handle Resources
 *
 * @author Sébastien Binda
 */
public interface IResourcesService {

    /**
     * Retrieve paged resources in database
     *
     * @param pMicroserviceName microservice
     * @param pPageable         the paging information
     * @return {@link Page} of {@link ResourcesAccess}
     * @throws ModuleException if error occurs!
     */
    Page<ResourcesAccess> retrieveRessources(String pMicroserviceName, Pageable pPageable) throws ModuleException;

    /**
     * Retrieve a {@link ResourcesAccess}
     *
     * @param pResourceId the if of the {@link ResourcesAccess} to retrieve
     * @return {@link ResourcesAccess}
     * @throws ModuleException if error occurs!
     */
    ResourcesAccess retrieveRessource(Long pResourceId) throws ModuleException;

    /**
     * Update the given resource {@link ResourcesAccess}
     *
     * @param pResourceToUpdate {@link ResourcesAccess} to update
     * @return {@link ResourcesAccess}
     * @throws ModuleException if error occurs!
     */
    ResourcesAccess updateResource(ResourcesAccess pResourceToUpdate) throws ModuleException;

    /**
     * Merge the given resources for the given microservice to the already configured ones. If the resource does not
     * exists, the default role given is configured for resources access.
     *
     * @param pResourcesToRegister list of {@link ResourceMapping} to register
     * @param pMicroserviceName    microservice owner of the resources to register
     * @throws ModuleException if error occurs!
     */
    void registerResources(final List<ResourceMapping> pResourcesToRegister, final String pMicroserviceName)
        throws ModuleException;

    /**
     * Retrieve all resources for the given microservice and the given controller name that can be managed by the specified role.
     * A role cannot managed a resource he cannot access.
     *
     * @param pMicroserviceName microservice name
     * @param pControllerName   controller name
     * @param roleName          the role name
     * @return List of {@link ResourcesAccess}
     */
    List<ResourcesAccess> retrieveMicroserviceControllerEndpoints(String pMicroserviceName,
                                                                  String pControllerName,
                                                                  String roleName);

    /**
     * Retreive microservice controllers names hat can be managed by the specified role.
     *
     * @param pMicroserviceName microservice name
     * @param roleName          the role name
     * @return Array of String (controllers names)
     */
    List<String> retrieveMicroserviceControllers(String pMicroserviceName, String roleName);

    /**
     * remove a resource access from a role and its descendants
     *
     * @param pRoleName          role name
     * @param pResourcesAccessId resource identifier
     * @throws ModuleException if error occurs!
     */
    void removeRoleResourcesAccess(String pRoleName, Long pResourcesAccessId) throws ModuleException;
}
