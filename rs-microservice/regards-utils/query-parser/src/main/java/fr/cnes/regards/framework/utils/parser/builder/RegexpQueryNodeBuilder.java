package fr.cnes.regards.framework.utils.parser.builder;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.framework.utils.parser.rule.RegexpPropertyRule;

public class RegexpQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        RegexpQueryNode node = (RegexpQueryNode) queryNode;
        return new RegexpPropertyRule(node.getFieldAsString(), node.getText().toString());
    }

}
