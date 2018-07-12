/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.search.dao.ISearchEngineConfRepository;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;

/**
 * Service to handle {@link SearchEngineConfiguration} entities.
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class SearchEngineConfigurationService implements ISearchEngineConfigurationService {

    @Autowired
    private ISearchEngineConfRepository repository;

    @Override
    public SearchEngineConfiguration createConf(SearchEngineConfiguration conf) {
        return repository.save(conf);
    }

    @Override
    public SearchEngineConfiguration updateConf(SearchEngineConfiguration conf) throws ModuleException {
        if (conf.getConfId() == null) {
            throw new EntityOperationForbiddenException("Cannot update entity does not exists");
        } else {
            retrieveConf(conf.getConfId());
        }
        return repository.save(conf);
    }

    @Override
    public void deleteConf(Long confId) throws ModuleException {
        retrieveConf(confId);
        repository.delete(confId);
    }

    @Override
    public SearchEngineConfiguration retrieveConf(Long confId) throws ModuleException {
        SearchEngineConfiguration conf = repository.findOne(confId);
        if (conf == null) {
            throw new EntityNotFoundException(confId, SearchEngineConfiguration.class);
        }
        return conf;
    }

    @Override
    public SearchEngineConfiguration retrieveConf(Optional<UniformResourceName> datasetUrn, String pluginId)
            throws ModuleException {
        SearchEngineConfiguration conf = null;
        String ds = null;
        if (datasetUrn.isPresent()) {
            ds = datasetUrn.get().toString();
            conf = repository.findByDatasetUrnAndConfigurationPluginId(datasetUrn.get().toString(), pluginId);
        }
        // If no conf found, then get a conf without dataset for the given pluginId
        if (conf == null) {
            conf = repository.findByDatasetUrnIsNullAndConfigurationPluginId(pluginId);
        }

        if (conf == null) {
            throw new EntityNotFoundException(String.format("SearchType=%s and Dataset=%s", pluginId, ds),
                    SearchEngineConfiguration.class);
        }
        return conf;
    }

}
