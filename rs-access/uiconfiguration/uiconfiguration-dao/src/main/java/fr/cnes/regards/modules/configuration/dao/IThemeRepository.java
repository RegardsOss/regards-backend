/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.configuration.domain.Theme;

/**
 *
 * Class IThemeRepository
 *
 * JPA Repository for Theme entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IThemeRepository extends CrudRepository<Theme, Long> {

    Page<Theme> findAll(Pageable pPageable);

    List<Theme> findByActiveTrue();

}
