/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
