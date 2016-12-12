/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 * Class IResourcesAccessRepository
 *
 * JPA Repository to access ResourcesAccess entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IResourcesAccessRepository extends JpaRepository<ResourcesAccess, Long> {

    /**
     *
     * find all resources accessible for every role given
     *
     * @param pRolesName
     *            Roles name
     * @param pPageable
     *            pagination information
     * @return {@link Page} of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    Page<ResourcesAccess> findDistinctByRolesNameIn(List<String> pRolesName, Pageable pPageable);

    /**
     *
     * find all resources accessible for every role given
     *
     * @param pRolesName
     *            Roles name
     * @return {@link Page} of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> findDistinctByRolesNameIn(List<String> pRolesName);

    /**
     *
     * Retrieve one resource by microservice, resource path and http verb
     *
     * @param pMicroservice
     *            Microservice name who own the resource
     * @param pResourceFullPath
     *            resource full path
     * @param pVerb
     *            HttpVerb of the resource
     * @return ResourcesAccess
     * @since 1.0-SNAPSHOT
     */
    ResourcesAccess findOneByMicroserviceAndResourceAndVerb(String pMicroservice, String pResourceFullPath,
            HttpVerb pVerb);

    /**
     *
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice
     *            Microservice name who own the resource
     * @return List<ResourcesAccess>
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> findByMicroservice(String pMicroservice);

    /**
     *
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice
     *            Microservice name who own the resource
     * @param pagination
     *            information
     * @return {@link Page} of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    Page<ResourcesAccess> findByMicroservice(String pMicroservice, Pageable pPageable);

    /**
     *
     * Retrieve paginaed resources for a given microservice and a list of roles.
     *
     * @param pMicroservice
     *            resources owner to retrieve
     * @param pRolesName
     *            resources roles to retrieve
     * @param pPageable
     *            pagination information
     * @return {@link Page} of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    Page<ResourcesAccess> findDistinctByMicroserviceAndRolesNameIn(String pMicroservice, List<String> pRolesName,
            Pageable pPageable);

}
