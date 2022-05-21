package fr.cnes.regards.framework.utils.parser.builder;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.framework.utils.parser.rule.PropertyRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class FieldQueryNodeBuilder implements IRuleBuilder {

    private static final String NULL = "null";

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        FieldQueryNode node = (FieldQueryNode) queryNode;
        if ((node.getTextAsString() == null) || node.getTextAsString().equalsIgnoreCase(NULL)) {
            return new PropertyRule(node.getFieldAsString(), null);
        } else {
            return new PropertyRule(node.getFieldAsString(), node.getTextAsString());
        }
    }
}
