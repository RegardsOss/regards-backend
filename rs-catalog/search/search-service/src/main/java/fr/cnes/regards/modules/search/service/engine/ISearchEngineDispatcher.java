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
package fr.cnes.regards.modules.search.service.engine;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.domain.plugin.IEntityLinkBuilder;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.dto.SearchRequest;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

/**
 * Search engine service contract<br/>
 * The service dispatches request to right search engine.
 *
 * @author Marc Sordi
 */
public interface ISearchEngineDispatcher {

    /**
     * Dispatch request to the right search engine according to specified search context
     */
    <T> ResponseEntity<T> dispatchRequest(SearchContext context, IEntityLinkBuilder linkBuilder) throws ModuleException;

    /**
     * Retrieve a search engine plugin instance for the given dataset and engine type.
     */
    ISearchEngine<?, ?, ?, ?> getSearchEngine(Optional<UniformResourceName> datasetUrn, String engineType)
        throws ModuleException;

    ICriterion computeComplexCriterion(SearchRequest searchRequest) throws ModuleException;

}
