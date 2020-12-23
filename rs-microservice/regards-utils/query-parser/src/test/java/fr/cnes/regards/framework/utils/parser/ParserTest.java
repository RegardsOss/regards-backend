package fr.cnes.regards.framework.utils.parser;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.utils.parser.rule.IRule;

public class ParserTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserTest.class);

    private static final String DATA = "data";

    private static final String DATA_TYPE = "type";

    private static final String DATA_TYPE_VALUE = "L2_RAD_GDR";

    private static final String DATA_TYPE_VALUE_REGEXP = "/L[1-3]_RAD_GD[A-Z]/";

    private static final String DATA_VALIDATION_FLAG = "validation_flag";

    private static final Boolean DATA_VALIDATION_FLAG_VALUE = Boolean.TRUE;

    private static final RuleParser RULE_PARSER = new RuleParser();

    @Test
    public void standardParserTest() throws QueryNodeException {

        StandardQueryParser qpHelper = new StandardQueryParser();
        @SuppressWarnings("unused")
        StandardQueryConfigHandler config = (StandardQueryConfigHandler) qpHelper.getQueryConfigHandler();
        // Samples
        parse(qpHelper, "data.type:\"L2_RAD_GDR\" AND data.validation_flag:true");
        parse(qpHelper, "data.type:\"L2_RAD_GDR\" OR data.validation_flag:true");
        parse(qpHelper, "date:[2007-12-03T10:15:30 TO 2007-12-03T11:15:30]");
    }

    private void parse(StandardQueryParser parser, String rule) throws QueryNodeException {

        Query query = parser.parse(rule, "defaultField");

        Set<Term> termSet = new HashSet<>();
        query.visit(QueryVisitor.termCollector(termSet));
        LOGGER.debug("end");
    }

    @Test
    public void matchingRules_example1() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:\"%s\" AND %s.%s:%s",
                                DATA,
                                DATA_TYPE,
                                DATA_TYPE_VALUE,
                                DATA,
                                DATA_VALIDATION_FLAG,
                                DATA_VALIDATION_FLAG_VALUE),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void regexpRules_example1() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:%s", DATA, DATA_TYPE, DATA_TYPE_VALUE_REGEXP),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void notMatchingRules_example1() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:\"%s\" AND %s.%s:%s",
                                DATA,
                                DATA_TYPE,
                                null,
                                DATA,
                                DATA_VALIDATION_FLAG,
                                DATA_VALIDATION_FLAG_VALUE),
                  example1(),
                  Boolean.FALSE);
    }

    private void parseRule(RuleParser parser, String ruleExpression, JsonObject target, Boolean match)
            throws QueryNodeException {
        // Parse rule(s)
        IRule rule = parser.parse(ruleExpression, "defaultField");
        Assert.assertNotNull("Rule must not be null!", rule);
        // Visit rule(s) to check if notification matches!
        JsonObjectMatchVisitor visitor = new JsonObjectMatchVisitor(target);
        Boolean result = rule.accept(visitor);
        LOGGER.debug("JSON object {}.", result ? "MATCHES" : "DOES NOT MATCH");
        Assert.assertEquals(result, match);
    }

    private JsonObject example1() {
        JsonObject o = new JsonObject();
        JsonObject data = new JsonObject();
        o.add(DATA, data);
        data.addProperty(DATA_TYPE, DATA_TYPE_VALUE);
        data.addProperty(DATA_VALIDATION_FLAG, DATA_VALIDATION_FLAG_VALUE);
        return o;
    }
}
