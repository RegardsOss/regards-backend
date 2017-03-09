/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.crawler.domain.criterion.AndCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ComparisonOperator;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.MatchType;
import fr.cnes.regards.modules.crawler.domain.criterion.OrCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ValueComparison;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
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

    @BeforeClass
    public static void init() {
        attributeModelService = Mockito.mock(IAttributeModelService.class);

        AttributeModel booleanAttributeModel = new AttributeModel();
        booleanAttributeModel.setName(ParserTestsUtils.booleanField);
        booleanAttributeModel.setType(AttributeType.BOOLEAN);

        AttributeModel integerAttributeModel = new AttributeModel();
        integerAttributeModel.setName(ParserTestsUtils.integerField);
        integerAttributeModel.setType(AttributeType.INTEGER);

        AttributeModel doubleAttributeModel = new AttributeModel();
        doubleAttributeModel.setName(ParserTestsUtils.doubleField);
        doubleAttributeModel.setType(AttributeType.DOUBLE);

        AttributeModel longAttributeModel = new AttributeModel();
        longAttributeModel.setName(ParserTestsUtils.longField);
        longAttributeModel.setType(AttributeType.LONG);

        AttributeModel stringAttributeModel = new AttributeModel();
        stringAttributeModel.setName(ParserTestsUtils.stringField);
        stringAttributeModel.setType(AttributeType.STRING);

        AttributeModel stringAttributeModel1 = new AttributeModel();
        stringAttributeModel1.setName(ParserTestsUtils.stringField1);
        stringAttributeModel1.setType(AttributeType.STRING);

        Mockito.when(attributeModelService.getAttributeModels())
                .thenReturn(Lists.newArrayList(booleanAttributeModel, integerAttributeModel, doubleAttributeModel,
                                               longAttributeModel, stringAttributeModel, stringAttributeModel1));

        parser = new RegardsQueryParser(attributeModelService);
    }

    @Test(expected = QueryNodeException.class)
    public void booleanMatchTest() throws QueryNodeException {
        String field = ParserTestsUtils.booleanField;
        Boolean value = true;
        String term = field + ":" + value;
        parser.parse(term, DEFAULT_FIELD);
    }

    @Test
    public void intMatchTest() throws QueryNodeException {
        final String field = ParserTestsUtils.integerField;
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
    public void doubleMatchTest() throws QueryNodeException {
        final String field = ParserTestsUtils.doubleField;
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
        Assert.assertEquals(crit.getValueComparisons(), expectedValueComparisons);
    }

    @Test
    public void stringMatchTest() throws QueryNodeException {
        final String key = ParserTestsUtils.stringField;
        final String val = "HarryPotter";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);

        final StringMatchCriterion crit = (StringMatchCriterion) criterion;
        Assert.assertEquals(key, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(val, crit.getValue());
    }

    @Test
    public void stringPhraseMatchTest() throws QueryNodeException {
        final String key = ParserTestsUtils.stringField;
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
    public void andMatchTest() throws QueryNodeException {
        String key0 = ParserTestsUtils.stringField;
        String key1 = ParserTestsUtils.stringField1;
        final ICriterion criterion = parser.parse(key0 + ":val1 AND " + key1 + ":val2", DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof AndCriterion);

        final AndCriterion crit = (AndCriterion) criterion;
        Assert.assertEquals(2, crit.getCriterions().size());
        Assert.assertTrue(crit.getCriterions().get(0) instanceof StringMatchCriterion);
        Assert.assertTrue(crit.getCriterions().get(1) instanceof StringMatchCriterion);
    }

    @Test
    public void orMatchTest() throws QueryNodeException {
        String key0 = ParserTestsUtils.stringField;
        String key1 = ParserTestsUtils.stringField1;
        final ICriterion criterion = parser.parse(key0 + ":val1 OR " + key1 + ":val2", DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof OrCriterion);

        final OrCriterion crit = (OrCriterion) criterion;
        Assert.assertEquals(2, crit.getCriterions().size());
        Assert.assertTrue(crit.getCriterions().get(0) instanceof StringMatchCriterion);
        Assert.assertTrue(crit.getCriterions().get(1) instanceof StringMatchCriterion);
    }

    @Test(expected = QueryNodeException.class)
    public void unsupportedSyntaxtTest() throws QueryNodeException {
        parser.parse("field:value~", DEFAULT_FIELD);
    }

    @Test(expected = QueryNodeException.class)
    public void termRangeTest() throws QueryNodeException {
        final String field = "movie";
        final String lowerInclusion = "{";
        final String lowerValue = "Armageddon";
        final String upperValue = "World War Z";
        final String upperInclusion = "}";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        parser.parse(term, DEFAULT_FIELD);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pointRangeIntegerInclusiveTest() throws QueryNodeException {
        final String field = ParserTestsUtils.integerField;
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
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.GREATER, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Integer>(ComparisonOperator.LESS, lowerValue));
        Assert.assertEquals(crit.getValueComparisons(), expectedValueComparisons);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pointRangeIntegerExclusiveTest() throws QueryNodeException {
        final String field = ParserTestsUtils.integerField;
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
        Assert.assertEquals(crit.getValueComparisons(), expectedValueComparisons);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pointRangeDoubleSemiInclusiveTest() throws QueryNodeException {
        final String field = ParserTestsUtils.doubleField;
        final String lowerInclusion = "{";
        final Double lowerValue = 0.00001;
        final Double upperValue = 0.99999;
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Double> crit = (RangeCriterion<Double>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Double>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Double>(ComparisonOperator.GREATER, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Double>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(crit.getValueComparisons(), expectedValueComparisons);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pointRangeLongTest() throws QueryNodeException {
        final String field = ParserTestsUtils.longField;
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
        expectedValueComparisons.add(new ValueComparison<Long>(ComparisonOperator.GREATER, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Long>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(crit.getValueComparisons(), expectedValueComparisons);
    }

    @Test
    public void parenthesisAroundAllTest() throws QueryNodeException {
        final String field = ParserTestsUtils.stringField;
        final String value = "StarWars";
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
    public void parenthesisAroundOrTest() throws QueryNodeException {
        final String field = ParserTestsUtils.stringField;
        final String term = field + ":(StarWars OR HarryPotter)";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof OrCriterion);
    }

}
