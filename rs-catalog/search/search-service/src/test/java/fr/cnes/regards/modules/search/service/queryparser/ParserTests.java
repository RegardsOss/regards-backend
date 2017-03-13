/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.crawler.domain.criterion.AndCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ComparisonOperator;
import fr.cnes.regards.modules.crawler.domain.criterion.DateRangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.MatchType;
import fr.cnes.regards.modules.crawler.domain.criterion.OrCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ValueComparison;
import fr.cnes.regards.modules.search.service.attributemodel.AttributeModelService;
import fr.cnes.regards.modules.search.service.attributemodel.IAttributeModelClient;
import fr.cnes.regards.modules.search.service.attributemodel.IAttributeModelService;

/**
 * @author Marc Sordi
 * @author Xavier-Alexandre Brochard
 *
 */
public class ParserTests {

    private static String DEFAULT_FIELD = "defaultField";

    private static RegardsQueryParser parser;

    private static IAttributeModelService attributeModelService;

    private static IAttributeModelClient attributeModelClient;

    @BeforeClass
    public static void init() throws EntityNotFoundException {
        attributeModelClient = Mockito.mock(IAttributeModelClient.class);
        attributeModelService = new AttributeModelService(attributeModelClient);

        Mockito.when(attributeModelClient.getAttributeModels()).thenReturn(ParserTestsUtils.LIST);

        parser = new RegardsQueryParser(attributeModelService);
    }

    @Test(expected = QueryNodeException.class)
    @Purpose("Tests queries like isTrue:false")
    public void booleanMatchTest() throws QueryNodeException {
        String field = ParserTestsUtils.BOOLEAN_FIELD;
        Boolean value = true;
        String term = field + ":" + value;
        parser.parse(term, DEFAULT_FIELD);
    }

    @Test
    @Purpose("Tests queries like altitude:8848")
    public void intMatchTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_FIELD;
        final Integer value = 8848;
        final String term = field + ":" + value;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof IntMatchCriterion);

        final IntMatchCriterion crit = (IntMatchCriterion) criterion;
        Assert.assertEquals(field, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(value, crit.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    @Purpose("Tests queries like bpm:128.0")
    public void doubleMatchTest() throws QueryNodeException {
        final String field = ParserTestsUtils.DOUBLE_FIELD;
        final Double value = 145.6;
        final String term = field + ":" + value;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Double> crit = (RangeCriterion<Double>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Double>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons
                .add(new ValueComparison<Double>(ComparisonOperator.GREATER_OR_EQUAL, Math.nextDown(value)));
        expectedValueComparisons.add(new ValueComparison<Double>(ComparisonOperator.LESS_OR_EQUAL, Math.nextUp(value)));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @Test
    @Purpose("Tests queries like title:harrypotter")
    public void stringMatchTest() throws QueryNodeException {
        final String key = ParserTestsUtils.STRING_FIELD;
        final String val = "harrypotter";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);

        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(key, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(val, crit.getValue());
    }

    @Test
    @Purpose("Tests queries like title:\"harry potter\"")
    public void stringPhraseMatchTest() throws QueryNodeException {
        final String key = ParserTestsUtils.STRING_FIELD;
        final String val = "\"a phrase query\"";
        final String expectedAfterParse = "a phrase query";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);

        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(key, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(expectedAfterParse, crit.getValue());
    }

    @Test
    @Purpose("Tests queries like title:*potter")
    public void wildcardLeading() throws QueryNodeException {
        final String key = ParserTestsUtils.STRING_FIELD;
        final String val = "*potter";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);

        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(key, crit.getName());
        Assert.assertEquals(MatchType.STARTS_WITH, crit.getType());
        Assert.assertEquals("potter", crit.getValue());
    }

    @Test
    @Purpose("Tests queries like title:harry*")
    public void wildcardTrailing() throws QueryNodeException {
        final String key = ParserTestsUtils.STRING_FIELD;
        final String val = "harry*";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);

        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(key, crit.getName());
        Assert.assertEquals(MatchType.ENDS_WITH, crit.getType());
        Assert.assertEquals("harry", crit.getValue());
    }

    @Test(expected = QueryNodeException.class)
    @Purpose("Tests queries like title:har*ter")
    public void wildcardMiddleTest() throws QueryNodeException {
        final String key = ParserTestsUtils.STRING_FIELD;
        final String val = "har*ter";
        parser.parse(key + ":" + val, DEFAULT_FIELD);
    }

    @Test
    @Purpose("Tests queries like title:harrypotter AND author:jkrowling")
    public void andMatchTest() throws QueryNodeException {
        String key0 = ParserTestsUtils.STRING_FIELD;
        String key1 = ParserTestsUtils.STRING_FIELD_1;
        final ICriterion criterion = parser.parse(key0 + ":harrypotter AND " + key1 + ":jkrowling", DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof AndCriterion);

        final AndCriterion crit = (AndCriterion) criterion;
        Assert.assertEquals(2, crit.getCriterions().size());
        Assert.assertTrue(crit.getCriterions().get(0) instanceof StringMatchCriterion);
        Assert.assertTrue(crit.getCriterions().get(1) instanceof StringMatchCriterion);
    }

    @Test
    @Purpose("Tests queries like title:harrypotter OR author:jkrowling")
    public void orMatchTest() throws QueryNodeException {
        String key0 = ParserTestsUtils.STRING_FIELD;
        String key1 = ParserTestsUtils.STRING_FIELD_1;
        final ICriterion criterion = parser.parse(key0 + ":val1 OR " + key1 + ":val2", DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof OrCriterion);

        final OrCriterion crit = (OrCriterion) criterion;
        Assert.assertEquals(2, crit.getCriterions().size());
        Assert.assertTrue(crit.getCriterions().get(0) instanceof StringMatchCriterion);
        Assert.assertTrue(crit.getCriterions().get(1) instanceof StringMatchCriterion);
    }

    @Test(expected = QueryNodeException.class)
    @Purpose("Tests queries like title:harry~")
    public void proximityTest() throws QueryNodeException {
        parser.parse("field:value~", DEFAULT_FIELD);
    }

    @Test(expected = QueryNodeException.class)
    @Purpose("Tests queries like title:{harrypotter TO starwars}")
    public void stringRangeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.STRING_RANGE_FIELD;
        final String lowerInclusion = "{";
        final String lowerValue = "harrypotter";
        final String upperValue = "starwars";
        final String upperInclusion = "}";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        parser.parse(term, DEFAULT_FIELD);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like altitude:{90 TO 120}")
    public void integerRangeExclusiveTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_RANGE_FIELD;
        final String lowerInclusion = "{";
        final Integer lowerValue = 90;
        final Integer upperValue = 120;
        final String upperInclusion = "}";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Integer> crit = (RangeCriterion<Integer>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Integer>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like altitude:[90 TO 120]")
    public void integerRangeInclusiveTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_RANGE_FIELD;
        final String lowerInclusion = "[";
        final Integer lowerValue = 0;
        final Integer upperValue = 2;
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Integer> crit = (RangeCriterion<Integer>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Integer>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like bpm:{128.0 TO 145]")
    public void doubleRangeSemiInclusiveTest() throws QueryNodeException {
        final String field = ParserTestsUtils.DOUBLE_RANGE_FIELD;
        final String lowerInclusion = "{";
        final Double lowerValue = 128d;
        final Double upperValue = 145d;
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Double> crit = (RangeCriterion<Double>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Double>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Double>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Double>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like distance:{0 TO 88]")
    public void longRangeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LONG_RANGE_FIELD;
        final String lowerInclusion = "{";
        final Long lowerValue = 0L;
        final Long upperValue = 88L;
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Long> crit = (RangeCriterion<Long>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Long>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Long>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Long>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @Test
    @Purpose("Tests queries like date:[2007-12-03T10:15:30 TO 2007-12-03T11:15:30]")
    public void localDateTimeRangeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LOCAL_DATE_TIME_RANGE_FIELD;
        final String lowerInclusion = "{";
        final LocalDateTime lowerValue = LocalDateTime.now().minusHours(1);
        final LocalDateTime upperValue = LocalDateTime.now();
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof DateRangeCriterion);
        final DateRangeCriterion crit = (DateRangeCriterion) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<LocalDateTime>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons
                .add(new ValueComparison<LocalDateTime>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        expectedValueComparisons.add(new ValueComparison<LocalDateTime>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like date:{* TO 2007-12-03T10:15:30}")
    public void localDateTimeLtTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LOCAL_DATE_TIME_RANGE_FIELD;
        final LocalDateTime upperValue = LocalDateTime.now();
        final String term = field + ":{ * TO " + upperValue + "}";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<LocalDateTime> crit = (RangeCriterion<LocalDateTime>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<LocalDateTime>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<LocalDateTime>(ComparisonOperator.LESS, upperValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like date:{* TO 2007-12-03T10:15:30]")
    public void localDateTimeLeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LOCAL_DATE_TIME_RANGE_FIELD;
        final LocalDateTime upperValue = LocalDateTime.now();
        final String term = field + ":{ * TO " + upperValue + "]";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<LocalDateTime> crit = (RangeCriterion<LocalDateTime>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<LocalDateTime>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<LocalDateTime>(ComparisonOperator.LESS_OR_EQUAL, upperValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like date:{2007-12-03T10:15:30 TO *}")
    public void localDateTimeGtTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LOCAL_DATE_TIME_RANGE_FIELD;
        final LocalDateTime lowerValue = LocalDateTime.now();
        final String term = field + ":{" + lowerValue + " TO *}";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<LocalDateTime> crit = (RangeCriterion<LocalDateTime>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<LocalDateTime>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<LocalDateTime>(ComparisonOperator.GREATER, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like date:[2007-12-03T10:15:30 TO *}")
    public void localDateTimeGeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LOCAL_DATE_TIME_RANGE_FIELD;
        final LocalDateTime lowerValue = LocalDateTime.now();
        final String term = field + ":[ " + lowerValue + " TO *}";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<LocalDateTime> crit = (RangeCriterion<LocalDateTime>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<LocalDateTime>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons
                .add(new ValueComparison<LocalDateTime>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like altitude:{* TO 1}")
    public void integerLtTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_RANGE_FIELD;
        final Integer upperValue = 1;
        final String term = field + ":{ * TO " + upperValue + "}";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Integer> crit = (RangeCriterion<Integer>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Integer>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.LESS, upperValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like altitude:{* TO 1]")
    public void integerLeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_RANGE_FIELD;
        final Integer upperValue = 1;
        final String term = field + ":{ * TO " + upperValue + "]";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Integer> crit = (RangeCriterion<Integer>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Integer>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.LESS_OR_EQUAL, upperValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like altitude:{1 TO *}")
    public void integerGtTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_RANGE_FIELD;
        final Integer lowerValue = 1;
        final String term = field + ":{" + lowerValue + " TO *}";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Integer> crit = (RangeCriterion<Integer>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Integer>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.GREATER, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like altitude:[1 TO *}")
    public void integerGeTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_RANGE_FIELD;
        final Integer lowerValue = 1;
        final String term = field + ":[ " + lowerValue + " TO *}";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Integer> crit = (RangeCriterion<Integer>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Integer>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @Test
    @Purpose("Tests queries like (title:harrypotter)")
    public void parenthesisAroundAllTest() throws QueryNodeException {
        final String field = ParserTestsUtils.STRING_FIELD;
        final String value = "harrypotter";
        final String term = "(" + field + ":" + value + ")";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);
        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(field, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(value, crit.getValue());
    }

    @Test
    @Purpose("Tests queries like title:(harrypotter OR starwars)")
    public void parenthesisAroundOrTest() throws QueryNodeException {
        final String field = ParserTestsUtils.STRING_FIELD;
        final String term = field + ":(harrypotter OR starwars)";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof OrCriterion);
    }

    @Test
    @Purpose("Tests queries like cast:danielradcliffe")
    public void stringArrayTest() throws QueryNodeException {
        final String field = ParserTestsUtils.STRING_ARRAY_FIELD;
        final String value = "danielradcliffe";
        final String term = field + ":" + value;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);
        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(field, crit.getName());
        Assert.assertEquals(MatchType.CONTAINS, crit.getType());
        Assert.assertEquals(value, crit.getValue());
    }

    @Test
    @Purpose("Tests queries like years:2001")
    public void integerArrayTest() throws QueryNodeException {
        final String field = ParserTestsUtils.INTEGER_ARRAY_FIELD;
        final Integer value = 2001;
        final String term = field + ":" + value;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof IntMatchCriterion);
        final IntMatchCriterion crit = (IntMatchCriterion) criterion;
        Assert.assertEquals(field, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(value, crit.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Purpose("Tests queries like duration:159")
    public void doubleArrayTest() throws QueryNodeException {
        final String field = ParserTestsUtils.DOUBLE_ARRAY_FIELD;
        final Double value = 159d;
        final String term = field + ":" + value;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Double> crit = (RangeCriterion<Double>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Double>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons
                .add(new ValueComparison<Double>(ComparisonOperator.GREATER_OR_EQUAL, Math.nextDown(value)));
        expectedValueComparisons.add(new ValueComparison<Double>(ComparisonOperator.LESS_OR_EQUAL, Math.nextUp(value)));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }

    @Test
    @Purpose("Tests queries like releases:[2001-11-04T00:00:00 TO 2001-11-16T23:59:59]")
    public void containsDateBetweenTest() throws QueryNodeException {
        final String field = ParserTestsUtils.LOCAL_DATE_TIME_ARRAY;
        final LocalDateTime lowerValue = LocalDateTime.parse("2001-11-04T00:00:00");
        final LocalDateTime upperValue = LocalDateTime.parse("2001-11-16T23:59:59");
        final String lowerInclusion = "[";
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof DateRangeCriterion);
        final DateRangeCriterion crit = (DateRangeCriterion) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<LocalDateTime>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons
                .add(new ValueComparison<LocalDateTime>(ComparisonOperator.GREATER_OR_EQUAL, lowerValue));
        expectedValueComparisons.add(new ValueComparison<LocalDateTime>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(expectedValueComparisons, crit.getValueComparisons());
    }
}
