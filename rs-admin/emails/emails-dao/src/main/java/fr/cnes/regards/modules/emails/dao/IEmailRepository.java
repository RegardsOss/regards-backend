/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.dao;

import org.springframework.data.domain.Example;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Interface for an JPA auto-generated CRUD repository managing Emails.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author CS SI
 */
public interface IEmailRepository extends CrudRepository<Email, Long> {

}
