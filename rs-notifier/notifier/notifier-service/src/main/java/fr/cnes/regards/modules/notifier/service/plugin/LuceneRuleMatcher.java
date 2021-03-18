package fr.cnes.regards.modules.notifier.service.plugin;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.parser.JsonObjectMatchVisitor;
import fr.cnes.regards.framework.utils.parser.RuleParser;
import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Lucene rule matcher", id = LuceneRuleMatcher.PLUGIN_ID,
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class LuceneRuleMatcher implements IRuleMatcher {

    public static final String LUCENE_RULE_NAME = "lucene_rule";

    private static final RuleParser RULE_PARSER = new RuleParser();

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneRuleMatcher.class);

    public static final String PLUGIN_ID = "LuceneRuleMatcher";

    @PluginParameter(name = LUCENE_RULE_NAME, label = "lucene expression to match")
    private String luceneRule;

    @Override
    public boolean match(JsonObject jsonObject) {
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
