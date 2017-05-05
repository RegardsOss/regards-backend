/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.role.DefaultRole;
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
            RequestMethod pVerb);

    /**
     *
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice
     *            Microservice name who own the resource
     * @return List of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> findByMicroservice(String pMicroservice);

    /**
     *
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice
     *            Microservice name who own the resource
     * @param pPageable
     *            the pagination information
     * @return {@link Page} of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    Page<ResourcesAccess> findByMicroservice(String pMicroservice, Pageable pPageable);

    /**
     *
     * Retrieve all resource for a given microservice and a given controller
     *
     * @param pMicroservice
     *            microservice name
     * @param pControllerSimpleName
     *            controller name
     * @param pExcludedDefaultRole excluded default role
     * @return List of {@link ResourcesAccess}
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> findByMicroserviceAndControllerSimpleNameAndDefaultRoleNotOrderByResource(
            String pMicroservice, String pControllerSimpleName, DefaultRole pExcludedDefaultRole);

    /**
     * Retrieve the list of controller names for a given microservice name
     *
     * @param pMicroservice
     * @return Array of String
     * @since 1.0-SNAPSHOT
     */
    @Query("select distinct controllerSimpleName from ResourcesAccess where microservice = ?1 and defaultRole <> INSTANCE_ADMIN")
    List<String> findAllControllersByMicroservice(String pMicroservice);

}
