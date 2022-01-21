/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.access.services.dao.ui;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;

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
     * @param isLinkedToAllEntities
     *            [true|false]
     * @param pageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByLinkedToAllEntities(Boolean isLinkedToAllEntities, Pageable pageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration}
     *
     * @param isActive
     *            [true|false]
     * @param pageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByActive(Boolean isActive, Pageable pageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration} associated to all entities througth linkedToAllEntities parameter
     *
     * @param isActive
     *            [true|false]
     * @param isLinkedToAllEntities
     *            [true|false]
     * @param pageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByActiveAndLinkedToAllEntities(Boolean isActive, Boolean isLinkedToAllEntities,
            Pageable pageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration} associated to all entities througth linkedToAllEntities parameter
     * with the given type
     *
     * @param pluginType type
     * @param isActive [true|false]
     * @param isLinkedToAllEntities [true|false]
     * @param pageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByPluginDefinitionTypeAndActiveAndLinkedToAllEntities(UIPluginTypesEnum pluginType,
            Boolean isActive, Boolean isLinkedToAllEntities, Pageable pageable);

    /**
     *
     * Find all actives {@link UIPluginConfiguration} of type SERVICE and associated to all entities througth
     * linkedToAllEntities parameter
     *
     * @param isActive
     *            [true|false]
     * @param isLinkedToAllEntities
     *            [true|false]
     * @param pluginDefinitionType
     *            type {@link UIPluginTypesEnum}
     * @return {@link List} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPHSOT
     */
    List<UIPluginConfiguration> findByActiveAndLinkedToAllEntitiesAndPluginDefinitionType(Boolean isActive,
            Boolean isLinkedToAllEntities, UIPluginTypesEnum pluginDefinitionType);

    /**
     *
     * Find all {@link UIPluginConfiguration} associated to the given {@link UIPluginDefinition}
     *
     * @param plugin
     *            {@link UIPluginDefinition}
     * @param pageable
     * @return {@link Page} of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> findByPluginDefinition(UIPluginDefinition plugin, Pageable pageable);

    Page<UIPluginConfiguration> findByPluginDefinitionTypeAndActive(UIPluginTypesEnum pluginType, Boolean isActive,
            Pageable pageable);

    Page<UIPluginConfiguration> findByPluginDefinitionTypeAndLinkedToAllEntities(UIPluginTypesEnum pluginType,
            Boolean isLinkedToAllEntities, Pageable pageable);

    Page<UIPluginConfiguration> findByPluginDefinitionType(UIPluginTypesEnum pluginType, Pageable pageable);

    long countByPluginDefinition(UIPluginDefinition plugin);

    default boolean hasPluginConfigurations(UIPluginDefinition plugin) {
        return countByPluginDefinition(plugin) > 0;
    }
}
