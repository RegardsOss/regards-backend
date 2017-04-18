/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;

/**
 *
 * Class IPluginConfigurationRepository
 *
 * JPA Repository for PluginConfiguration entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IUIPluginConfigurationRepository extends JpaRepository<UIPluginConfiguration, Long> {

    Page<UIPluginConfiguration> findByLinkedToAllEntities(Boolean pIsLinkedToAllEntities, Pageable pPageable);

    Page<UIPluginConfiguration> findByActive(Boolean pIsActive, Pageable pPageable);

    Page<UIPluginConfiguration> findByActiveAndLinkedToAllEntities(Boolean pIsActive, Boolean pIsLinkedToAllEntities,
            Pageable pPageable);

    Page<UIPluginConfiguration> findByPluginDefinition(UIPluginDefinition pPlugin, Pageable pPageable);

}
