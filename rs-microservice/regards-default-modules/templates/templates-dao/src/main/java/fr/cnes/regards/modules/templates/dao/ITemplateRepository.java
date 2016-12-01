/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.dao;

import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.templates.domain.Template;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link Account}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface ITemplateRepository extends JpaRepository<Template, Long> {

    /**
     * Find the single {@link Template} with passed <code>code</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pCode
     *            The {@link Template}'s <code>code</code>
     * @return An optional template
     */
    Optional<Template> findOneByCode(String pCode);

}
