package fr.cnes.regards.framework.utils.parser.builder;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.framework.utils.parser.rule.OrRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;

import java.util.List;

public class OrQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        OrQueryNode node = (OrQueryNode) queryNode;

        List<QueryNode> children = node.getChildren();
        if (children != null) {
            OrRule rule = new OrRule();
            for (QueryNode child : children) {
                Object obj = child.getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
                if (obj != null) {
                    rule.add((IRule) obj);
                }
            }
            return rule;
        }

        throw new QueryNodeException(new MessageImpl("Empty OR is not supported"));
    }

}
