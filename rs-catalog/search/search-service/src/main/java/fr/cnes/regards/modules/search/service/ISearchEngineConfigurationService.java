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
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interface for {@link SearchEngineConfiguration}s service handler.
 *
 * @author Sébastien Binda
 */
public interface ISearchEngineConfigurationService {

    /**
     * Initialize searchengines for the given project.
     */
    void initDefaultSearchEngine(Class<?> legacySearchEnginePluginClass);

    /**
     * Creates a new {@link SearchEngineConfiguration}.
     *
     * @param conf {@link SearchEngineConfiguration}
     * @return new {@link SearchEngineConfiguration} created.
     */
    SearchEngineConfiguration createConf(SearchEngineConfiguration conf) throws ModuleException;

    /**
     * Update the given {@link SearchEngineConfiguration}
     *
     * @param conf {@link SearchEngineConfiguration} to update
     * @return {@link SearchEngineConfiguration} updated
     * @throws ModuleException
     */
    SearchEngineConfiguration updateConf(SearchEngineConfiguration conf) throws ModuleException;

    /**
     * Delete the {@link SearchEngineConfiguration} associated to the given id
     *
     * @param confId if of the {@link SearchEngineConfiguration} to delete
     * @throws ModuleException
     */
    void deleteConf(Long confId) throws ModuleException;

    /**
     * Retrieve all {@link SearchEngineConfiguration} matching the given engineType
     *
     * @param engineType engine type of the {@link SearchEngineConfiguration}s to search for
     * @throws ModuleException
     */
    Page<SearchEngineConfiguration> retrieveConfs(Optional<String> engineType, Pageable page) throws ModuleException;

    /**
     * Retrieve a {@link SearchEngineConfiguration} by his id.
     *
     * @param confId id of the {@link SearchEngineConfiguration} to retrieve
     * @return {@link SearchEngineConfiguration}
     * @throws ModuleException
     */
    SearchEngineConfiguration retrieveConf(Long confId) throws ModuleException;

    /**
     * Retrieve a {@link SearchEngineConfiguration} associated to te given dataset and PluginId
     *
     * @param datasetUrn dataset associated to the conf
     * @param pluginId   Plugin identifier
     * @return {@link SearchEngineConfiguration}
     * @throws ModuleException
     */
    SearchEngineConfiguration retrieveConf(Optional<UniformResourceName> datasetUrn, String pluginId)
        throws ModuleException;

    /**
     * Retrieve all {@link SearchEngineConfiguration}.
     *
     * @return all search engine configurations
     */
    List<SearchEngineConfiguration> retrieveAllConfs();
}
