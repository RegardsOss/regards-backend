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
package fr.cnes.regards.modules.search.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;

/**
 * Performs an OpenSearch request with the passed string query.
 * @author Xavier-Alexandre Brochard
 */
public interface ICatalogSearchService { // NOSONAR

    /**
     * Perform an OpenSearch request on a type.
     * @param allParams all query parameters
     * @param searchKey the search key containing the search type and the result type
     * @param facets the facets applicable
     * @param pPageable the page
     * @return the page of elements matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    <S, R extends IIndexable> FacetPage<R> search(Map<String, String> allParams, SearchKey<S, R> searchKey,
            String[] facets, final Pageable pPageable) throws SearchException;

    DocFilesSummary computeDatasetsSummary(Map<String, String> allParams, SimpleSearchKey<DataObject> searchKey,
            String datasetIpId, String[] fileTypes) throws SearchException;

    /**
     *
     * @param urn identifier of the entity we are looking for
     * @param <E> concrete type of AbstractEntity
     * @return the entity
     * @throws EntityOperationForbiddenException
     */
    <E extends AbstractEntity> E get(UniformResourceName urn)
            throws EntityOperationForbiddenException, EntityNotFoundException;
}
