/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link MetaData}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IMetaDataRepository extends JpaRepository<MetaData, Long> {

}
