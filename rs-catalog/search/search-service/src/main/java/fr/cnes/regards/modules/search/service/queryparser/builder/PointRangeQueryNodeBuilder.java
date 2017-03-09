/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser.builder;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointRangeQueryNode;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;

/**
 * Builds a {@link RangeCriterion} from a {@link PointRangeQueryNode} object.
 *
 * @see org.apache.lucene.queryparser.flexible.standard.builders.PointRangeQueryNodeBuilder#build(QueryNode)
 * @author Xavier-Alexandre Brochard
 */
public class PointRangeQueryNodeBuilder implements ICriterionQueryBuilder {

    @Override
    public ICriterion build(final QueryNode pQueryNode) throws QueryNodeException {
        final PointRangeQueryNode numericRangeNode = (PointRangeQueryNode) pQueryNode;

        final PointQueryNode lowerNumericNode = numericRangeNode.getLowerBound();
        final PointQueryNode upperNumericNode = numericRangeNode.getUpperBound();

        final Number lowerNumber = lowerNumericNode.getValue();
        final Number upperNumber = upperNumericNode.getValue();

        final PointsConfig pointsConfig = numericRangeNode.getPointsConfig();
        final Class<? extends Number> numberType = pointsConfig.getType();
        final String field = StringUtils.toString(numericRangeNode.getField());

        if (Integer.class.equals(numberType)) {
            return ICriterion.between(field, (Integer) lowerNumber, (Integer) upperNumber);
        } else
            if (Long.class.equals(numberType)) {
                return ICriterion.between(field, (Long) lowerNumber, (Long) upperNumber);
            } else
                if (Float.class.equals(numberType)) {
                    return ICriterion.between(field, (Float) lowerNumber, (Float) upperNumber);
                } else
                    if (Double.class.equals(numberType)) {
                        return ICriterion.between(field, (Double) lowerNumber, (Double) upperNumber);
                    } else {
                        throw new QueryNodeException(
                                new MessageImpl(QueryParserMessages.UNSUPPORTED_NUMERIC_DATA_TYPE, numberType));
                    }
    }

}
