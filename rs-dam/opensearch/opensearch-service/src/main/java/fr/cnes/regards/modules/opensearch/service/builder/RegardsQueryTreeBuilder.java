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
package fr.cnes.regards.modules.opensearch.service.builder;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.*;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;

/**
 * Define the REGARDS specific query nodes building strategies.
 *
 * @author Marc Sordi
 * @author Xavier-Alexandre Brochard
 */
public class RegardsQueryTreeBuilder extends QueryTreeBuilder implements ICriterionQueryBuilder {

    /**
     * Constructor
     *
     * @param finder attribute finder
     */
    public RegardsQueryTreeBuilder(IAttributeFinder finder) {
        // Register builder
        super.setBuilder(QuotedFieldQueryNode.class, new QuotedFieldQueryNodeBuilder(finder));
        super.setBuilder(FieldQueryNode.class, new FieldQueryNodeBuilder(finder));
        super.setBuilder(AndQueryNode.class, new AndQueryNodeBuilder());
        super.setBuilder(OrQueryNode.class, new OrQueryNodeBuilder());
        super.setBuilder(ModifierQueryNode.class, new ModifierQueryNodeBuilder());
        super.setBuilder(TermRangeQueryNode.class, new TermRangeQueryNodeBuilder(finder));
        super.setBuilder(WildcardQueryNode.class, new WildcardQueryNodeBuilder(finder));
        super.setBuilder(GroupQueryNode.class, new GroupQueryNodeBuilder());
        super.setBuilder(FuzzyQueryNode.class, new UnsupportedQueryNodeBuilder());
        super.setBuilder(BooleanQueryNode.class, new BooleanNodeQueryBuilder());
        super.setBuilder(RegexpQueryNode.class, new RegexpQueryNodeBuilder(finder));
    }

    @Override
    public ICriterion build(final QueryNode queryNode) throws QueryNodeException {
        return (ICriterion) super.build(queryNode);
    }

}
