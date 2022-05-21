package fr.cnes.regards.framework.utils.parser;

import fr.cnes.regards.framework.utils.parser.builder.RuleQueryTreeBuilder;
import fr.cnes.regards.framework.utils.parser.rule.IRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.QueryParserHelper;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.processors.StandardQueryNodeProcessorPipeline;

public class RuleParser extends QueryParserHelper {

    public RuleParser() {
        super(new StandardQueryConfigHandler(),
              new StandardSyntaxParser(),
              new StandardQueryNodeProcessorPipeline(null),
              new RuleQueryTreeBuilder());
        setEnablePositionIncrements(true);
        setAllowLeadingWildcard(true);
    }

    @Override
    public IRule parse(String query, String defaultField) throws QueryNodeException {
        return (IRule) super.parse(query, defaultField);
    }

    /**
     * Set to <code>true</code> to allow leading wildcard characters.
     * <p>
     * When set, <code>*</code> or <code>?</code> are allowed as the first character of a PrefixQuery and WildcardQuery.
     * Note that this can produce very slow queries on big indexes.
     * <p>
     * Default: false.
     */
    private final void setAllowLeadingWildcard(final boolean allowLeadingWildcard) {
        getQueryConfigHandler().set(StandardQueryConfigHandler.ConfigurationKeys.ALLOW_LEADING_WILDCARD,
                                    allowLeadingWildcard);
    }

    /**
     * Set to <code>true</code> to enable position increments in result query.
     * <p>
     * When set, result phrase and multi-phrase queries will be aware of position increments. Useful when e.g. a
     * StopFilter increases the position increment of the token that follows an omitted token.
     * <p>
     * Default: false.
     */
    private final void setEnablePositionIncrements(final boolean enabled) {
        getQueryConfigHandler().set(StandardQueryConfigHandler.ConfigurationKeys.ENABLE_POSITION_INCREMENTS, enabled);
    }
}
