/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import org.springframework.data.domain.Example;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link ResourcesAccess}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author CS SI
 */
public interface IResourcesAccessRepository extends CrudRepository<ResourcesAccess, Long> {

    ResourcesAccess findOneByMicroserviceAndResourceAndVerb(String pMicroservice, String pResourceFullPath,
            HttpVerb pVerb);

}
