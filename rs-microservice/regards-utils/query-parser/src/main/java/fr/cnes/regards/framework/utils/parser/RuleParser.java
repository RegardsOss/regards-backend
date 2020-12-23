package fr.cnes.regards.framework.utils.parser;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.QueryParserHelper;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.processors.StandardQueryNodeProcessorPipeline;

import fr.cnes.regards.framework.utils.parser.builder.RuleQueryTreeBuilder;
import fr.cnes.regards.framework.utils.parser.rule.IRule;

public class RuleParser extends QueryParserHelper {

    public RuleParser() {
        super(new StandardQueryConfigHandler(), new StandardSyntaxParser(),
              new StandardQueryNodeProcessorPipeline(null), new RuleQueryTreeBuilder());
    }

    @Override
    public IRule parse(String query, String defaultField) throws QueryNodeException {
        return (IRule) super.parse(query, defaultField);
    }
}
