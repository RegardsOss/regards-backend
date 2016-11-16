/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

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
public interface IResourcesAccessRepository extends CrudRepository<ResourcesAccess, Long> {

    /**
     *
     * Retrieve one resource by microservice, resource path and http verb
     *
     * @param pMicroservice
     *            Microservice name who own the resource
     * @param resourceFullPath
     *            resource full path
     * @param pVerb
     *            HttpVerb of the resource
     * @return ResourcesAccess
     * @since 1.0-SNAPSHOT
     */
    ResourcesAccess findOneByMicroserviceAndResourceAndVerb(String pMicroservice, String resourceFullPath,
            HttpVerb pVerb);

    /**
     *
     * Retrieve all resource for a given microservice
     *
     * @param pMicroservice
     * @return List<ResourcesAccess>
     * @since 1.0-SNAPSHOT
     */
    List<ResourcesAccess> findByMicroservice(String pMicroservice);

}
