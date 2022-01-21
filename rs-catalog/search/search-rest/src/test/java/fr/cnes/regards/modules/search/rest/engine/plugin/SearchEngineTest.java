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
package fr.cnes.regards.modules.search.rest.engine.plugin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.domain.plugin.IEntityLinkBuilder;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;

/**
 * Plugin for test purposes.
 * @author Sébastien Binda
 */
@Plugin(id = SearchEngineTest.ENGINE_ID, author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Test search engine", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "1.0.0")
public class SearchEngineTest implements ISearchEngine<Object, Object, Object, List<String>> {

    public static final String ENGINE_ID = "testengine";

    public static final String DATASET_PARAM = "dataset";

    @PluginParameter(name = DATASET_PARAM, optional = true, label = "dataset")
    private String associatedDataset;

    @Override
    public boolean supports(SearchType searchType) {
        return true;
    }

    @Override
    public ResponseEntity<Object> search(SearchContext context, ISearchEngine<?, ?, ?, ?> parser,
            IEntityLinkBuilder linkBuilder) throws ModuleException {
        return new ResponseEntity<Object>(associatedDataset, HttpStatus.OK);
    }

    @Override
    public ICriterion parse(SearchContext context) throws ModuleException {
        return ICriterion.all();
    }

    @Override
    public ResponseEntity<Object> getEntity(SearchContext context, IEntityLinkBuilder linkBuilder)
            throws ModuleException {
        return new ResponseEntity<Object>(associatedDataset, HttpStatus.OK);
    }

}
