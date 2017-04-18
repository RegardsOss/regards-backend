/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.Plugin;
import fr.cnes.regards.modules.configuration.domain.PluginTypesEnum;

/**
 *
 * Class IPluginRepository
 *
 * JPA Repository for Plugin entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IPluginRepository extends JpaRepository<Plugin, Long> {

    @Override
    Page<Plugin> findAll(Pageable pPageable);

    Page<Plugin> findByType(PluginTypesEnum pType, Pageable pPageable);

}
