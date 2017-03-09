/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.List;

import org.springframework.http.HttpRequest;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 *
 * Defines the contract on filtering methods
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "plugin interface for filter plugins")
public interface IFilter {

    /**
     *
     * add criterion according to the plugin implementation and the HttpRequest
     *
     * @param pRequest
     *            HttpRequest from which additionnal criterion might be added
     * @param pEnhencedCriterion
     *            list of base criterion from the request plus the ones added by other filters
     * @return enhancedCriterion plus the one(s) added by the filter implementation
     */
    public List<ICriterion> addFilter(HttpRequest pRequest, List<ICriterion> pEnhencedCriterion);

    /**
     * allow the caller to know if the given Dataset can be treated by this implementation
     *
     * @param pCandidate
     * @return if this implementation can be applied to the given dataset
     */
    public boolean isRelevant(Dataset pCandidate);

}
