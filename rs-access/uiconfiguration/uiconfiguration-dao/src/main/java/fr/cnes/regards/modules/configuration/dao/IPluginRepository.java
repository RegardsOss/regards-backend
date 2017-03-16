/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.configuration.domain.Plugin;

/**
 *
 * Class IPluginRepository
 *
 * JPA Repository for Plugin entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IPluginRepository extends CrudRepository<Plugin, Long> {

    Page<Plugin> findAll(Pageable pPageable);

}
