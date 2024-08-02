/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.utils.parser.builder;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointRangeQueryNode;

/**
 * Builds a {@link IRule} from a {@link PointRangeQueryNode} object.
 *
 * @author Sébastien Binda
 * @see GroupQueryNodeBuilder#build(QueryNode)
 */
public class GroupQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(final QueryNode queryNode) throws QueryNodeException {
        final GroupQueryNode groupNode = (GroupQueryNode) queryNode;
        return (IRule) (groupNode).getChild().getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
    }

}
