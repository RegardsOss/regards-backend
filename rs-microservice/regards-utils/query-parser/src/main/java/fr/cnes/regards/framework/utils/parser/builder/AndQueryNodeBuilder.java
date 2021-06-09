package fr.cnes.regards.framework.utils.parser.builder;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;

import fr.cnes.regards.framework.utils.parser.rule.AndRule;
import fr.cnes.regards.framework.utils.parser.rule.IRule;

public class AndQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        AndQueryNode node = (AndQueryNode) queryNode;

        List<QueryNode> children = node.getChildren();
        if (children != null) {
            AndRule rule = new AndRule();
            for (QueryNode child : children) {
                Object obj = child.getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
                if (obj != null) {
                    rule.add((IRule) obj);
                }
            }
            return rule;
        }

        throw new QueryNodeException(new MessageImpl("Empty AND is not supported"));
    }

}
