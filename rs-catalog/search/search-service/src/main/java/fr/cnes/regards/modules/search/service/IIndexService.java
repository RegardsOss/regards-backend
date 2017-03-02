package fr.cnes.regards.modules.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;

/**
 * This service performs a request to the Elasticsearch indexation database based on the passed search criterion and
 * returns the result.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IIndexService {

    /**
     * Searching first page of elements giving page size without facets nor sort. The used index corresponds to the
     * current tenant at runtime.
     *
     * @param pClass
     *            class of document type
     * @param pPageSize
     *            page size
     * @param criterion
     *            search criterion
     * @return first result page containing max page size documents
     */
    <T> Page<T> search(Class<T> pClass, int pPageSize, ICriterion criterion);

    /**
     * Searching specified page of elements from index (for first call use {@link #searchAllLimited(String, Class, int)}
     * method) without facets nor sort. <b>This method fails if asked for offset greater than 10000 (Elasticsearch
     * limitation).</b> The used index corresponds to the current tenant at runtime.
     *
     * @param pIndex
     *            index
     * @param pClass
     *            class of document type
     * @param pPageRequest
     *            page request (use {@link Page#nextPageable()} method for example)
     * @param pCriterion
     *            search criterion
     * @param <T>
     *            class of document type
     * @return specified result page
     */
    <T> Page<T> search(Class<T> pClass, Pageable pPageRequest, ICriterion criterion);
}
