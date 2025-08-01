/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.utils.parser.builder;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.framework.utils.parser.rule.NumberRangePropertyRule;
import jakarta.annotation.Nullable;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;

import java.math.BigDecimal;

/**
 * This class builds an {@link IRule} from a lucene "term range". Lucene term ranges look like
 * <code>{12 TO 24}</code>, <code>[12 TO 24]</code>, </code><code>{* TO 50]</code>, <code>{250 TO *}</code>.
 * <p>
 * This builder only supports numeric ranges, because the target model (IRule) only supports numeric ranges. It would
 * be possible to extend to other types such as date ranges or lexicographic ranges.
 *
 * @author Julien Canches
 */
public class TermRangeQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        TermRangeQueryNode rangeNode = (TermRangeQueryNode) queryNode;

        return new NumberRangePropertyRule(rangeNode.getLowerBound().getField().toString(),
                                           getDecimalValue(rangeNode.getLowerBound()),
                                           getDecimalValue(rangeNode.getUpperBound()),
                                           rangeNode.isLowerInclusive(),
                                           rangeNode.isUpperInclusive());
    }

    public static @Nullable BigDecimal getDecimalValue(FieldQueryNode node) {
        String value = node.getTextAsString();
        if (value.isEmpty()) {
            return null;
        }
        return new BigDecimal(value);
    }

}
