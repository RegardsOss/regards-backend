/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
public interface IThemeRepository extends JpaRepository<Theme, Long> {

    List<Theme> findByActiveTrue();

}
