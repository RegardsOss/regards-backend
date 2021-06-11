package fr.cnes.regards.framework.utils.parser.builder;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryBuilder;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class BooleanNodeQueryBuilder implements QueryBuilder {

    @Override
    public Object build(QueryNode queryNode) throws QueryNodeException {
        BooleanQueryNode node = (BooleanQueryNode) queryNode;
        List<QueryNode> children = node.getChildren();
        if (children != null) {
            if (children.size() != 1) {
                throw new QueryNodeException(new MessageImpl(
                        "Indeterminate relationship between criteria. Resolve this issue by adding OR or AND between criteria or surrounding values containing spaces with quotes"));
            }
            QueryNode child = children.get(0);
            return child.getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
        }

        throw new QueryNodeException(new MessageImpl("Empty BooleanQueryNode is not supported")); // This should not happens
    }
}
