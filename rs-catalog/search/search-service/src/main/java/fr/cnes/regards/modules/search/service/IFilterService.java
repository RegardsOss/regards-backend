package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;

/**
 * This service translates the search criterion of an OpenSearch format query into an ElasticSearch request.<br>
 * This is achieved by using {@link IFilter} type plugins.
 *
 * @author Xavier-Alexandre Brochard
 */
@FunctionalInterface
public interface IFilterService {

    /**
     * Translates the search criterion of an OpenSearch format query into an ElasticSearch request.
     * 
     * @param pOpenSearchRequest
     * @return
     */
    ICriterion getFilters(Object pOpenSearchRequest);

}
