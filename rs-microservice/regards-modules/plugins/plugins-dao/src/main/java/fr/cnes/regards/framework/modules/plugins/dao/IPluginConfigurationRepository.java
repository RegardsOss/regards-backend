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
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * {@link PluginConfiguration} repository
 *
 * @author Christophe Mertz
 */
@Repository
public interface IPluginConfigurationRepository extends JpaRepository<PluginConfiguration, Long> {

    /**
     * Find a {@link List} of {@link PluginConfiguration} for a plugin
     *
     * @param pPluginId the plugin identifier
     * @return a {@link List} of {@link PluginConfiguration}
     */
    List<PluginConfiguration> findByPluginIdOrderByPriorityOrderDesc(String pPluginId);

    /**
     * Find a {@link List} of active {@link PluginConfiguration} for a plugin
     *
     * @param pPluginId the plugin identifier
     * @return a {@link List} of active {@link PluginConfiguration}
     */
    List<PluginConfiguration> findByPluginIdAndActiveTrueOrderByPriorityOrderDesc(String pPluginId);

    @Query("from PluginConfiguration pc join fetch pc.parameters where parent_conf_id=:id")
    PluginConfiguration findOneWithPluginParameter(@Param("id") Long pId);

    /**
     * @param pConfigurationLabel
     * @return the plugin configuration which label is the given label in parameter
     */
    PluginConfiguration findOneByLabel(String pConfigurationLabel);

    /**
     * Find a plugin configuration loading its parameters and dynamic values
     *
     * @param id pluginConfiguration id
     * @return a PluginConfiguration without lazy relations
     */
    @EntityGraph(attributePaths = { "parameters", "parameters.dynamicsValues" })
    PluginConfiguration findById(Long id);

    @Override
    @Modifying
    @Query(value = "TRUNCATE {h-schema}t_plugin_configuration CASCADE", nativeQuery = true)
    void deleteAll();
}
