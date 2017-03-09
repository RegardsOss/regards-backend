/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser.builder;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;

/**
 * Builds a {@link RangeCriterion} from a {@link TermRangeQueryNode} object.
 *
 * @author Xavier-Alexandre Brochard
 */
public class TermRangeQueryNodeBuilder implements ICriterionQueryBuilder {

    @Override
    public ICriterion build(final QueryNode pQueryNode) throws QueryNodeException {
        final TermRangeQueryNode rangeNode = (TermRangeQueryNode) pQueryNode;
        final FieldQueryNode upper = rangeNode.getUpperBound();
        final FieldQueryNode lower = rangeNode.getLowerBound();

        final String field = StringUtils.toString(rangeNode.getField());
        String lowerText = lower.getTextAsString();
        String upperText = upper.getTextAsString();

        if (lowerText.length() == 0) {
            lowerText = null;
        }

        if (upperText.length() == 0) {
            upperText = null;
        }

        return ICriterion.between(field, lowerText, upperText, rangeNode.isLowerInclusive(),
                                  rangeNode.isUpperInclusive());
    }

}
