package fr.cnes.regards.modules.search.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * Performs an OpenSearch request with the passed string query.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface ICatalogSearchService {

    /**
     * Perform an OpenSearch request on a type.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pSearchType
     *            the indexed type on which we perform the search (not necessary the type returned!). Use
     *            <code>null</code> if search on all types.
     * @param pResultClass
     *            the returned class. Most of the time, the same as the search type, expect for joint searches.
     * @param pFacets
     *            the facets applicable
     * @param pPageable
     *            the page
     * @return the page of elements matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    <T extends IIndexable> Page<T> search(String pQ, EntityType pSearchType, Class<T> pResultClass,
            List<String> pFacets, final Pageable pPageable) throws SearchException;
}
