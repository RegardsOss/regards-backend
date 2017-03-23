package fr.cnes.regards.modules.search.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.domain.SearchType;

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
     *            the indexed type on which we perform the search (not necessary the type returned!)
     * @param pResultClass
     *            the returned class. Most of the time, the same as the search type, expect for joint searches.
     * @param pFacets
     *            the facets applicable
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of elements matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    <T extends AbstractEntity> Page<T> search(String pQ, SearchType pSearchType, Class<T> pResultClass,
            List<String> pFacets, final Pageable pPageable, final PagedResourcesAssembler<T> pAssembler)
            throws SearchException;
}
