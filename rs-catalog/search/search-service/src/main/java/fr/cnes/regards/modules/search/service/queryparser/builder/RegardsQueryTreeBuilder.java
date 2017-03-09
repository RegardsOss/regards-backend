/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser.builder;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;

/**
 * Define the REGARDS specific query nodes building strategies.
 *
 * @author Marc Sordi
 * @author Xavier-Alexandre Brochard
 */
public class RegardsQueryTreeBuilder extends QueryTreeBuilder implements ICriterionQueryBuilder {

    public RegardsQueryTreeBuilder() {

        // Register builder
        setBuilder(FieldQueryNode.class, new FieldQueryNodeBuilder());
        setBuilder(AndQueryNode.class, new AndQueryNodeBuilder());
        setBuilder(OrQueryNode.class, new OrQueryNodeBuilder());
        setBuilder(ModifierQueryNode.class, new ModifierQueryNodeBuilder());
        setBuilder(TermRangeQueryNode.class, new TermRangeQueryNodeBuilder());
        setBuilder(PointRangeQueryNode.class, new PointRangeQueryNodeBuilder());
        setBuilder(GroupQueryNode.class, new GroupQueryNodeBuilder());

        setBuilder(FuzzyQueryNode.class, new UnsupportedQueryNodeBuilder());

        // setBuilder(PointQueryNode.class, new FieldQueryNodeBuilder());
        // setBuilder(BooleanQueryNode.class, new BooleanQueryNodeBuilder());
        // setBuilder(LegacyNumericQueryNode.class, new DummyQueryNodeBuilder());
        // setBuilder(LegacyNumericRangeQueryNode.class, new LegacyNumericRangeQueryNodeBuilder());
        // setBuilder(BoostQueryNode.class, new BoostQueryNodeBuilder());
        // setBuilder(WildcardQueryNode.class, new WildcardQueryNodeBuilder());
        // setBuilder(TokenizedPhraseQueryNode.class, new PhraseQueryNodeBuilder());
        // setBuilder(MatchNoDocsQueryNode.class, new MatchNoDocsQueryNodeBuilder());
        // setBuilder(PrefixWildcardQueryNode.class,
        // new PrefixWildcardQueryNodeBuilder());
        // setBuilder(RegexpQueryNode.class, new RegexpQueryNodeBuilder());
        // setBuilder(SlopQueryNode.class, new SlopQueryNodeBuilder());
        // setBuilder(StandardBooleanQueryNode.class,
        // new StandardBooleanQueryNodeBuilder());
        // setBuilder(MultiPhraseQueryNode.class, new MultiPhraseQueryNodeBuilder());
        // setBuilder(MatchAllDocsQueryNode.class, new MatchAllDocsQueryNodeBuilder());
    }

    @Override
    public ICriterion build(final QueryNode queryNode) throws QueryNodeException {
        return (ICriterion) super.build(queryNode);
    }
}
