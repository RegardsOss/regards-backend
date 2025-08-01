package fr.cnes.regards.framework.utils.parser.builder;

import fr.cnes.regards.framework.utils.parser.rule.AndRule;
import fr.cnes.regards.framework.utils.parser.rule.IRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;

import java.util.List;
import java.util.Objects;

public class AndQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        AndQueryNode node = (AndQueryNode) queryNode;

        List<QueryNode> children = node.getChildren();
        if (children != null) {
            return new AndRule(children.stream()
                                       .map(child -> (IRule) child.getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID))
                                       .filter(Objects::nonNull)
                                       .toList());
        }

        throw new QueryNodeException(new MessageImpl("Empty AND is not supported"));
    }

}
