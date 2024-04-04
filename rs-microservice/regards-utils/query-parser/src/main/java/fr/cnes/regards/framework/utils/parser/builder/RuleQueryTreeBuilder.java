/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.apache.lucene.queryparser.flexible.core.nodes.*;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;

/**
 * Build rule tree
 *
 * @author Marc SORDI
 */
public class RuleQueryTreeBuilder extends QueryTreeBuilder implements IRuleBuilder {

    public RuleQueryTreeBuilder() {

        // Register builder
        super.setBuilder(AndQueryNode.class, new AndQueryNodeBuilder());
        super.setBuilder(OrQueryNode.class, new OrQueryNodeBuilder());
        super.setBuilder(QuotedFieldQueryNode.class, new QuotedFieldQueryNodeBuilder());
        super.setBuilder(FieldQueryNode.class, new FieldQueryNodeBuilder());
        super.setBuilder(ModifierQueryNode.class, new ModifierQueryNodeBuilder());
        // setBuilder(TermRangeQueryNode.class, new TermRangeQueryNodeBuilder(finder));
        // setBuilder(WildcardQueryNode.class, new WildcardQueryNodeBuilder(finder));
        super.setBuilder(GroupQueryNode.class, new GroupQueryNodeBuilder());
        // setBuilder(FuzzyQueryNode.class, new UnsupportedQueryNodeBuilder());
        super.setBuilder(BooleanQueryNode.class, new BooleanNodeQueryBuilder());
        super.setBuilder(RegexpQueryNode.class, new RegexpQueryNodeBuilder());
    }

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        return (IRule) super.build(queryNode);
    }
}
