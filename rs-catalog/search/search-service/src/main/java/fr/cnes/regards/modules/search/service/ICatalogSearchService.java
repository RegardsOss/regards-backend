package fr.cnes.regards.modules.search.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;

/**
 * Performs an OpenSearch request with the passed string query.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface ICatalogSearchService { // NOSONAR

    /**
     * Perform an OpenSearch request on a type.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pSearchKey
     *            the search key containing the search type and the result type
     * @param pFacets
     *            the facets applicable
     * @param pPageable
     *            the page
     * @return the page of elements matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    <S, R extends IIndexable> Page<R> search(Map<String,String> pQ, SearchKey<S, R> pSearchKey, Map<String, FacetType> pFacets,
            final Pageable pPageable) throws SearchException;
}
