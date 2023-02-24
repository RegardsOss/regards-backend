package fr.cnes.regards.modules.notifier.service.plugin;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.parser.JsonObjectMatchVisitor;
import fr.cnes.regards.framework.utils.parser.RuleParser;
import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team",
        description = "Lucene rule matcher",
        id = LuceneRuleMatcher.PLUGIN_ID,
        version = "1.0.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class LuceneRuleMatcher implements IRuleMatcher {

    public static final String PAYLOAD_RULE_NAME = "payload_rule";

    public static final String METADATA_RULE_NAME = "metadata_rule";

    private static final RuleParser RULE_PARSER = new RuleParser();

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneRuleMatcher.class);

    public static final String PLUGIN_ID = "LuceneRuleMatcher";

    @PluginParameter(name = PAYLOAD_RULE_NAME, label = "lucene expression to match")
    private String payloadRule;

    @PluginParameter(name = METADATA_RULE_NAME, label = "lucene expression to match", optional = true)
    private String metadataRule;

    @Override
    public boolean match(JsonObject metadata, JsonObject payload) {
        return match(metadata, metadataRule) && match(payload, payloadRule);
    }

    private boolean match(JsonObject jsonObject, String luceneRule) {
        if (luceneRule == null) {
            return true;
        }
        try {
            // Parse rule(s)
            IRule rule = RULE_PARSER.parse(luceneRule, "defaultField");
            // Visit rule(s) to check if notification matches!
            JsonObjectMatchVisitor visitor = new JsonObjectMatchVisitor(jsonObject);
            return rule.accept(visitor);
        } catch (QueryNodeException e) {
            LOGGER.error(String.format("Lucene rule %s could not be parsed because of syntax issues", luceneRule), e);
            return false;
        }
    }
}
