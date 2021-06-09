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

    private static final String DATA_STRING = "type";

    private static final String DATA_STRING_VALUE = "L2_RAD_GDR";

    private static final String DATA_STRING_VALUE_REGEXP = "/L[1-3]_RAD_GD[A-Z]/";

    private static final String DATA_BOOLEAN = "validation_flag";

    private static final Boolean DATA_BOOLEAN_VALUE = Boolean.TRUE;

    private static final RuleParser RULE_PARSER = new RuleParser();

    private static final String DATA_LONG = "filesize";

    private static final Long DATA_LONG_VALUE = 33L;

    private static final String DATA_INTEGER = "product_counter";

    private static final String DATA_DATE = "ingestion_date";

    private static final Integer DATA_INTEGER_VALUE = 46;

    private static final String DATA_DATE_VALUE = "2020-02-23T06:54:23.056Z";

    private static final String DATA_DATE_VALUE_ESCAPED_FOR_QUERY = "2020-02-23T06\\:54\\:23.056Z";

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
    public void matchingRuleAnd() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:\"%s\" AND %s.%s:%s",
                                DATA, DATA_STRING, DATA_STRING_VALUE,
                                DATA, DATA_BOOLEAN, DATA_BOOLEAN_VALUE),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void matchingRuleOr() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:\"%s\" OR %s.%s:%s",
                                DATA, DATA_STRING, DATA_STRING_VALUE,
                                DATA, DATA_BOOLEAN, DATA_BOOLEAN_VALUE),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void regexpRulesEqualRegex() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:%s", DATA, DATA_STRING, DATA_STRING_VALUE_REGEXP),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void regexpRulesEqualLong() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:%s", DATA, DATA_LONG, DATA_LONG_VALUE),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void regexpRulesEqualInteger() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:%s", DATA, DATA_INTEGER, DATA_INTEGER_VALUE),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void regexpRulesEqualDate() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:%s", DATA, DATA_DATE, DATA_DATE_VALUE_ESCAPED_FOR_QUERY),
                  example1(),
                  Boolean.TRUE);
    }

    @Test
    public void notMatchingRulesString() throws QueryNodeException {
        parseRule(RULE_PARSER,
                  String.format("%s.%s:\"%s\"",
                                DATA, DATA_STRING,
                                null),
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
        Assert.assertEquals(match, result);
    }

    private JsonObject example1() {
        JsonObject o = new JsonObject();
        JsonObject data = new JsonObject();
        o.add(DATA, data);
        data.addProperty(DATA_STRING, DATA_STRING_VALUE);
        data.addProperty(DATA_BOOLEAN, DATA_BOOLEAN_VALUE);
        data.addProperty(DATA_LONG, DATA_LONG_VALUE);
        data.addProperty(DATA_INTEGER, DATA_INTEGER_VALUE);
        data.addProperty(DATA_DATE, DATA_DATE_VALUE);
        return o;
    }
}
