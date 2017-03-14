/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.filter;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.search.service.cache.IAttributeModelCache;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;

/**
 * Default implementation of the {@link IFilterPlugin}.<br>
 * This plugin interprets the research criterion and transforms them in ElasticSearch request.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "filter", author = "CSSI", description = "Interpretation of an OpenSearch query",
        version = "1.0.0-SNAPSHOT")
public class DefaultFilterPlugin implements IFilterPlugin {

    /**
     * A cache wrapping the {@link IAttributeModelClient} in order to limit the effective calls.
     */
    @Autowired
    private IAttributeModelCache attributeModelCache;

    /**
     * Set a dummy default field. It is not used in the defined REGARDS search API.
     */
    private static final String DEFAULT_FIELD = "defaultField";

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.filter.IFilterPlugin#addFilter(org.springframework.http.HttpRequest,
     * java.util.List)
     */
    @Override
    public List<ICriterion> addFilter(HttpRequest pRequest, List<ICriterion> pEnhencedCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.filter.IFilterPlugin#getFilters(java.lang.String)
     */
    @Override
    public ICriterion getFilters(String pOpenSearchRequest) throws QueryNodeException {
        RegardsQueryParser parser = new RegardsQueryParser(attributeModelCache);
        return parser.parse(pOpenSearchRequest, DEFAULT_FIELD);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.search.service.filter.IFilterPlugin#isRelevant(fr.cnes.regards.modules.entities.domain.
     * Dataset)
     */
    @Override
    public boolean isRelevant(Dataset pCandidate) {
        // TODO Auto-generated method stub
        return false;
    }

}
