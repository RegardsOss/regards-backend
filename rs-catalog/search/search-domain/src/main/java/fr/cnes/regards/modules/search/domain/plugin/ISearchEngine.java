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
package fr.cnes.regards.modules.search.domain.plugin;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

/**
 * Search engine plugin contract
 * @author Marc Sordi
 *
 * @param R search result type
 * @param E extra result type
 * @param T single entity type
 */
public interface ISearchEngine<R, E, T> {

    /**
     * Check if plugin supports the required search type. A plugin may support several search types.
     */
    boolean supports(SearchType searchType);

    /**
     * Engine search method
     */
    ResponseEntity<R> search(SearchContext context) throws ModuleException;

    /**
     * Parse query parameters and transform to {@link ICriterion} (available for all search method)<br/>
     * Use {@link ICriterion} as criterion builder.<br/>
     * <b>This method implementation is required for subsetting feature.</b>
     * @param queryParams all query parameters
     * @return {@link ICriterion}
     */
    ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException;

    /**
     * Additional extra path handling. Not supported by default
     */
    default ResponseEntity<E> extra(SearchContext context) throws ModuleException {
        throw new UnsupportedOperationException("Additional path handling not supported");
    }

    /**
     * Retrieve a single entity from its URN ({@link SearchContext#getUrn()})
     */
    ResponseEntity<T> getEntity(SearchContext context) throws ModuleException;
}
