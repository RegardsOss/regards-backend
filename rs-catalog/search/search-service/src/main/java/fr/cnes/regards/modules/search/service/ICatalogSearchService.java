/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;

/**
 * Performs an OpenSearch request with the passed string query.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface ICatalogSearchService { // NOSONAR

    /**
     * Perform an OpenSearch request on a type.
     *
     * @param allParams all query parameters
     * @param searchKey the search key containing the search type and the result type
     * @param facets the facets applicable
     * @param pPageable the page
     * @return the page of elements matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    <S, R extends IIndexable> FacetPage<R> search(Map<String, String> allParams, SearchKey<S, R> searchKey,
            String[] facets, final Pageable pPageable) throws SearchException;
}
