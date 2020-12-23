/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QuotedFieldQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;

import fr.cnes.regards.framework.utils.parser.rule.IRule;

/**
 * Build rule tree
 * @author Marc SORDI
 */
public class RuleQueryTreeBuilder extends QueryTreeBuilder implements IRuleBuilder {

    public RuleQueryTreeBuilder() {

        // Register builder
        setBuilder(AndQueryNode.class, new AndQueryNodeBuilder());
        //        setBuilder(OrQueryNode.class, new OrQueryNodeBuilder());
        setBuilder(QuotedFieldQueryNode.class, new QuotedFieldQueryNodeBuilder());
        setBuilder(FieldQueryNode.class, new FieldQueryNodeBuilder());
        setBuilder(ModifierQueryNode.class, new ModifierQueryNodeBuilder());
        //        setBuilder(TermRangeQueryNode.class, new TermRangeQueryNodeBuilder(finder));
        //        setBuilder(WildcardQueryNode.class, new WildcardQueryNodeBuilder(finder));
        //        setBuilder(GroupQueryNode.class, new GroupQueryNodeBuilder());
        //        setBuilder(FuzzyQueryNode.class, new UnsupportedQueryNodeBuilder());
        //        setBuilder(BooleanQueryNode.class, new BooleanNodeQueryBuilder());
        setBuilder(RegexpQueryNode.class, new RegexpQueryNodeBuilder());
    }

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        return (IRule) super.build(queryNode);
    }
}
