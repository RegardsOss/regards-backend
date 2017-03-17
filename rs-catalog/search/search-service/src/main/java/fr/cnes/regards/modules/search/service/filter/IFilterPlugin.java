/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.filter;

import org.springframework.http.HttpRequest;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

/**
 * Defines the contract on filtering methods.<br>
 * This plugin interprets a request in the OpenSearch format and transforms them in ElasticSearch request.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brocard
 */
@PluginInterface(description = "Plugin interface for filter plugins")
public interface IFilterPlugin {

    /**
     * Add criterion according to the plugin implementation and the HttpRequest
     *
     * @param pRequest
     *            HttpRequest from which additionnal criterion might be added
     * @param pCriterion
     *            list of base criterion from the request plus the ones added by other filters
     * @return enhancedCriterion plus the one(s) added by the filter implementation
     */
    public ICriterion addFilter(HttpRequest pRequest, ICriterion pCriterion);

    /**
     * Allow the caller to know if the given Dataset can be treated by this implementation
     *
     * @param pCandidate
     * @return if this implementation can be applied to the given dataset
     */
    public boolean isRelevant(Dataset pCandidate);

}
