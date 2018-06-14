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

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;

/**
 * Catalog search service interface. Service façade to DAM search module (directly included by catalog).
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 */
public interface ICatalogSearchService {

    /**
     * Perform an OpenSearch request on a type.
     * @param allParams all query parameters
     * @param searchKey the search key containing the search type and the result type
     * @param facets the facets applicable
     * @return the page of elements matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @Deprecated // Only use method with ICriterion
    <S, R extends IIndexable> FacetPage<R> search(MultiValueMap<String, String> allParams, SearchKey<S, R> searchKey,
            List<String> facets, Pageable pageable) throws SearchException;

    /**
     * Perform a business request on specified entity type
     * @param criterion business criterions
     * @param searchKey the search key containing the search type and the result type
     * @param facets applicable facets, may be <code>null</code>
     * @param pageable pagination properties
     * @return the page of elements matching the criterions
     */
    <S, R extends IIndexable> FacetPage<R> search(ICriterion criterion, SearchKey<S, R> searchKey, List<String> facets,
            Pageable pageable) throws SearchException;

    /**
     * Same as below but using {@link SearchType}
     */
    <R extends IIndexable> FacetPage<R> search(ICriterion criterion, SearchType searchType, List<String> facets,
            Pageable pageable) throws SearchException;

    /**
     * Compute summary for given request
     * @param allParams OpenSearch request
     * @param searchKey search key
     * @param dataset dataset concerned by the request
     * @param dataTypes file types on which to compute summary
     * @return summary
     */
    @Deprecated // Only use method with ICriterion
    DocFilesSummary computeDatasetsSummary(MultiValueMap<String, String> allParams,
            SimpleSearchKey<DataObject> searchKey, UniformResourceName dataset, List<DataType> dataTypes)
            throws SearchException;

    /**
     * Compute summary for given request
     * @param criterion business criterions
     * @param searchKey search key
     * @param dataset restriction to a specified dataset
     * @param dataTypes file types on which to compute summary
     * @return summary
     */
    DocFilesSummary computeDatasetsSummary(ICriterion criterion, SimpleSearchKey<DataObject> searchKey,
            UniformResourceName dataset, List<DataType> dataTypes) throws SearchException;

    /**
     * Same as below but using {@link SearchType}
     */
    DocFilesSummary computeDatasetsSummary(ICriterion criterion, SearchType searchType, UniformResourceName dataset,
            List<DataType> dataTypes) throws SearchException;

    /**
     * Retrieve entity
     * @param urn identifier of the entity we are looking for
     * @param <E> concrete type of AbstractEntity
     * @return the entity
     */
    <E extends AbstractEntity> E get(UniformResourceName urn)
            throws EntityOperationForbiddenException, EntityNotFoundException;

    /**
     * Retrieve given STRING property enumerated values (limited by maxCount, partial text contains and
     * openSearch request from allParams (as usual)).
     * @param propertyPath concerned STRING property path
     * @param allParams opensearch request
     * @param partialText text that property should contains (can be null)
     * @param maxCount maximum result count
     */
    @Deprecated // Only use method with ICriterion
    <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(MultiValueMap<String, String> allParams,
            SearchKey<T, T> searchKey, String propertyPath, int maxCount, String partialText) throws SearchException;

    /**
     * Retrieve property values for specified property name
     * @param criterion business criterions
     * @param searchKey the search key containing the search type and the result type
     * @param propertyPath target propertu
     * @param maxCount maximum result count
     * @param partialText text that property should contains (can be null)
     */
    <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
            SearchKey<T, T> searchKey, String propertyPath, int maxCount, String partialText) throws SearchException;

    /**
     * Same as below but using {@link SearchType}
     */
    List<String> retrieveEnumeratedPropertyValues(ICriterion criterion, SearchType searchType, String propertyPath,
            int maxCount, String partialText) throws SearchException;
}
