package fr.cnes.regards.framework.utils.parser.builder;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class QuotedFieldQueryNodeBuilder extends FieldQueryNodeBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        return super.build(queryNode);
    }
}
