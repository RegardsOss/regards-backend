package fr.cnes.regards.framework.utils.parser.builder;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode.Modifier;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.framework.utils.parser.rule.NotRule;

public class ModifierQueryNodeBuilder implements IRuleBuilder {

    @Override
    public IRule build(QueryNode queryNode) throws QueryNodeException {
        ModifierQueryNode node = (ModifierQueryNode) queryNode;
        if (node.getModifier().equals(Modifier.MOD_NOT)) {
            return new NotRule((IRule) node.getChild().getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID));
        } else {
            return (IRule) node.getChild().getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
        }
    }
}
