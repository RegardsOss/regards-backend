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
package fr.cnes.regards.modules.search.rest.engine.plugin.legacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;

/**
 * Common legacy search class
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractLegacySearch<T, E> implements ISearchEngine<T, E> {

    /**
     * Query parameter for facets
     */
    protected static final String FACETS = "facets";

    /**
     * Pagination property
     */
    protected static final String PAGE_NUMBER = "page";

    /**
     * Pagination property
     */
    protected static final String PAGE_SIZE = "size";

    /**
     * Query parser
     */
    @Autowired
    protected IOpenSearchService openSearchService;

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        return openSearchService.parse(queryParams);
    }

}
