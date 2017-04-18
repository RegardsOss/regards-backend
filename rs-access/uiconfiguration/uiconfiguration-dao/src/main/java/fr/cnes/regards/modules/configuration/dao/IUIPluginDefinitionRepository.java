/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

/**
 *
 * Class IPluginRepository
 *
 * JPA Repository for Plugin entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IUIPluginDefinitionRepository extends JpaRepository<UIPluginDefinition, Long> {

    Page<UIPluginDefinition> findByType(UIPluginTypesEnum pType, Pageable pPageable);

}
