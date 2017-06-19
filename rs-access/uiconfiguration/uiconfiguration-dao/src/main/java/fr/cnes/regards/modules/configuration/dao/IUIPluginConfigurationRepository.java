/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

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

    /**
     *
     * Find {@link UIPluginConfiguration} associated to all entities througth linkedToAllEntities parameter
     *
     * @param pIsLinkedToAllEntities
     *            [true|false]
     * @param pPageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByLinkedToAllEntities(Boolean pIsLinkedToAllEntities, Pageable pPageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration}
     *
     * @param pIsActive
     *            [true|false]
     * @param pPageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByActive(Boolean pIsActive, Pageable pPageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration} associated to all entities througth linkedToAllEntities parameter
     *
     * @param pIsActive
     *            [true|false]
     * @param pIsLinkedToAllEntities
     *            [true|false]
     * @param pPageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByActiveAndLinkedToAllEntities(Boolean pIsActive, Boolean pIsLinkedToAllEntities,
            Pageable pPageable);

    /**
    *
    * Find all actives {@link UIPluginConfiguration} associated to all entities througth linkedToAllEntities parameter with the given type
    *
    * @param pPluginType type
    * @param pIsActive [true|false]
    * @param pIsLinkedToAllEntities [true|false]
    * @param pPageable
    * @return {@link Page} of {@link UIPluginConfiguration}
    * @since 1.0-SNAPSHOT
    */
    Page<UIPluginConfiguration> findByPluginDefinitionTypeAndActiveAndLinkedToAllEntities(UIPluginTypesEnum pPluginType,
            Boolean pIsActive, Boolean pIsLinkedToAllEntities, Pageable pPageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration} of type SERVICE and associated to all entities througth
     * linkedToAllEntities parameter
     *
     * @param pIsActive
     *            [true|false]
     * @param pIsLinkedToAllEntities
     *            [true|false]
     * @param pPluginDefinitionType
     *            type {@link UIPluginTypesEnum}
     * @return {@link List} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPHSOT
     */
    List<UIPluginConfiguration> findByActiveAndLinkedToAllEntitiesAndPluginDefinitionType(Boolean pIsActive,
            Boolean pIsLinkedToAllEntities, UIPluginTypesEnum pPluginDefinitionType);

    /**
     *
     * Find all {@link UIPluginConfiguration} associated to the given {@link UIPluginDefinition}
     *
     * @param pPlugin
     *            {@link UIPluginDefinition}
     * @param pPageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByPluginDefinition(UIPluginDefinition pPlugin, Pageable pPageable);

    Page<UIPluginConfiguration> findByPluginDefinitionTypeAndActive(UIPluginTypesEnum pPluginType, Boolean pIsActive,
            Pageable pPageable);

    Page<UIPluginConfiguration> findByPluginDefinitionTypeAndLinkedToAllEntities(UIPluginTypesEnum pPluginType,
            Boolean pIsLinkedToAllEntities, Pageable pPageable);

    Page<UIPluginConfiguration> findByPluginDefinitionType(UIPluginTypesEnum pPluginType, Pageable pPageable);

}
