/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.Layout;

/**
 *
 * Class ILayoutRepository
 *
 * JPA Repository for Layout entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface ILayoutRepository extends JpaRepository<Layout, Long> {

    /**
     *
     * Retrieve layout for the given application id.
     *
     * @param pApplicationId
     * @return {@link Layout}
     * @since 1.0-SNAPSHOT
     */
    Optional<Layout> findByApplicationId(String pApplicationId);

}
