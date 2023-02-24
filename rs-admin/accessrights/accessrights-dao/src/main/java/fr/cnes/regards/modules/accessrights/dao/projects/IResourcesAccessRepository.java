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
package fr.cnes.regards.modules.accessrights.dao.projects;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Class IResourcesAccessRepository
 * <p>
 * JPA Repository to access ResourcesAccess entities
 *
 * @author Sébastien Binda
 */
public interface IResourcesAccessRepository extends JpaRepository<ResourcesAccess, Long> {

    /**
     * Retrieve one resource by microservice, resource path and http verb
     *
     * @param pMicroservice     Microservice name who own the resource
     * @param pResourceFullPath resource full path
     * @param pVerb             HttpVerb of the resource
     * @return ResourcesAccess
     */
    ResourcesAccess findOneByMicroserviceAndResourceAndVerb(String pMicroservice,
                                                            String pResourceFullPath,
                                                            RequestMethod pVerb);

    /**
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice Microservice name who own the resource
     * @return List of {@link ResourcesAccess}
     */
    List<ResourcesAccess> findByMicroservice(String pMicroservice);

    /**
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice Microservice name who own the resource
     * @param pPageable     the pagination information
     * @return {@link Page} of {@link ResourcesAccess}
     */
    Page<ResourcesAccess> findByMicroservice(String pMicroservice, Pageable pPageable);

    /**
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice        Microservice name who own the resource
     * @param pExcludedDefaultRole role to exclude
     * @param pPageable            the pagination information
     * @return {@link Page} of {@link ResourcesAccess}
     */
    Page<ResourcesAccess> findByMicroserviceAndDefaultRoleNot(String pMicroservice,
                                                              DefaultRole pExcludedDefaultRole,
                                                              Pageable pPageable);

    /**
     * Returns a {@link Page} of entities meeting the paging restriction provided in the {@code Pageable} object.
     *
     * @param pExcludedDefaultRole role to exclude
     * @return a page of entities
     */
    Page<ResourcesAccess> findByDefaultRoleNot(DefaultRole pExcludedDefaultRole, Pageable pageable);

    /**
     * Retrieve all resources for a given microservice and a given controller
     *
     * @param pMicroservice         microservice name
     * @param pControllerSimpleName controller name
     * @param pExcludedDefaultRole  excluded default role
     * @return List of {@link ResourcesAccess}
     */
    List<ResourcesAccess> findByMicroserviceAndControllerSimpleNameAndDefaultRoleNotOrderByResource(String pMicroservice,
                                                                                                    String pControllerSimpleName,
                                                                                                    DefaultRole pExcludedDefaultRole);

    /**
     * Retrieve all resources for a given microservice, a given controller that can be managed by a specified role
     *
     * @param pMicroservice         microservice name
     * @param pControllerSimpleName controller name
     * @param pExcludedDefaultRole  excluded default role
     * @param roleName              role name
     * @return {@link List} of {@link ResourcesAccess} that can be managed by specified role
     */
    @Query(value = "select * from {h-schema}t_resources_access res, {h-schema}ta_resource_role resrole, {h-schema}t_role role  where microservice = ?1 and controller_name = ?2 and defaultrole <> 'INSTANCE_ADMIN' and res.id = resrole.resource_id and resrole.role_id = role.id and role.name = ?3 order by res.resource",
           nativeQuery = true)
    List<ResourcesAccess> findManageableResources(String pMicroservice, String pControllerSimpleName, String roleName);

    /**
     * Retrieve the list of controller names for a given microservice name
     *
     * @return Array of String
     */
    @Query(
        "select distinct controllerSimpleName from ResourcesAccess where microservice = ?1 and defaultRole <> 'INSTANCE_ADMIN'")
    List<String> findAllControllersByMicroservice(String pMicroservice);

    /**
     * Retrieve all controllers for a given microservice that can be managed by a specified role
     *
     * @param pMicroservice microservice name
     * @param roleName      role name
     */
    @Query(value = "select distinct controller_name from {h-schema}t_resources_access res, {h-schema}ta_resource_role resrole, {h-schema}t_role role  where microservice = ?1 and defaultrole <> 'INSTANCE_ADMIN' and res.id = resrole.resource_id and resrole.role_id = role.id and role.name = ?2 order by controller_name",
           nativeQuery = true)
    List<String> findManageableControllers(String pMicroservice, String roleName);
}
