package fr.cnes.regards.modules.notifier.service.plugin;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
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

    @PluginParameter(name = PAYLOAD_RULE_NAME, label = "lucene expression to match", optional = true)
    private String payloadRule;

    @PluginParameter(name = METADATA_RULE_NAME, label = "lucene expression to match", optional = true)
    private String metadataRule;

    IRule computedPayloadRule = null;

    IRule computedMetadataRule = null;

    @Override
    public boolean match(JsonObject metadata, JsonObject payload) {
        return match(metadata, computedMetadataRule) && match(payload, computedPayloadRule);
    }

    @PluginInit
    public void init() throws QueryNodeException {
        // Parse rule(s)
        if (payloadRule != null) {
            computedPayloadRule = RULE_PARSER.parse(payloadRule, "defaultField");
        }
        if (metadataRule != null) {
            computedMetadataRule = RULE_PARSER.parse(metadataRule, "defaultField");
        }
    }

    private boolean match(JsonObject jsonObject, IRule computedLuceneRule) {
        if (computedLuceneRule == null) {
            return true;
        }
        // Visit rule(s) to check if notification matches!
        JsonObjectMatchVisitor visitor = new JsonObjectMatchVisitor(jsonObject);
        return computedLuceneRule.accept(visitor);
    }
}
