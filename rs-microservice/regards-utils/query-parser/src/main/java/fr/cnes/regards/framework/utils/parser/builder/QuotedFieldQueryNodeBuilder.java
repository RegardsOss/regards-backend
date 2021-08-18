package fr.cnes.regards.framework.utils.parser.builder;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

import fr.cnes.regards.framework.utils.parser.rule.IRule;

public class QuotedFieldQueryNodeBuilder extends FieldQueryNodeBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        return super.build(queryNode);
    }
}
