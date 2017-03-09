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
import fr.cnes.regards.modules.crawler.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ComparisonOperator;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.MatchType;
import fr.cnes.regards.modules.crawler.domain.criterion.OrCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ValueComparison;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
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
        Mockito.when(attributeModelService.getAttributeModels()).thenReturn(Lists.newArrayList(new AttributeModel()));
        parser = new RegardsQueryParser(attributeModelService);
    }

    @Test
    public void booleanMatchTest() throws QueryNodeException {
        String field = "isCool";
        Boolean value = true;
        String term = field + ":" + value;
        ICriterion criterion = parser.parse(term, DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof BooleanMatchCriterion);
        BooleanMatchCriterion crit = (BooleanMatchCriterion) criterion;
        Assert.assertEquals(field, crit.getName());
        Assert.assertEquals(MatchType.EQUALS, crit.getType());
        Assert.assertEquals(value, crit.getValue());
    }

    @Test
    public void intMatchTest() throws QueryNodeException {
        final String field = "altitude";
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
    public void doubleMatchTest() throws QueryNodeException {
        final String field = "bpm";
        final Double value = 145.6;
        final String term = field + ":" + value;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        @SuppressWarnings("unchecked")
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
        final String key = "key";
        final String val = "val";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof StringMatchCriterion);
        final StringMatchCriterion smc = (StringMatchCriterion) criterion;
        Assert.assertEquals(key, smc.getName());
        Assert.assertEquals(MatchType.EQUALS, smc.getType());
        Assert.assertEquals(val, smc.getValue());
    }

    @Test
    public void stringPhraseMatchTest() throws QueryNodeException {
        final String key = "key";
        final String val = "\"a phrase query\"";
        final ICriterion criterion = parser.parse(key + ":" + val, DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
    }

    @Test
    public void andMatchTest() throws QueryNodeException {
        final ICriterion criterion = parser.parse("key1:val1 AND key2:val2", DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof AndCriterion);
    }

    @Test
    public void orMatchTest() throws QueryNodeException {
        final ICriterion criterion = parser.parse("key1:val1 OR key2:val2", DEFAULT_FIELD);
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof OrCriterion);
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
        final String field = "duration";
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
        final String field = "power";
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
        final String field = "percentage";
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
        final String field = "speed_in_mph";
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

    @SuppressWarnings("unchecked")
    @Test
    public void pointRangeFloatTest() throws QueryNodeException {
        final String field = "rotation";
        final String lowerInclusion = "{";
        final Float lowerValue = 0.1f;
        final Float upperValue = 0.9f;
        final String upperInclusion = "]";
        final String term = field + ":" + lowerInclusion + lowerValue + " TO " + upperValue + upperInclusion;
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof RangeCriterion);
        final RangeCriterion<Float> crit = (RangeCriterion<Float>) criterion;

        Assert.assertEquals(field, crit.getName());
        final Set<ValueComparison<Float>> expectedValueComparisons = new HashSet<>();
        expectedValueComparisons.add(new ValueComparison<Float>(ComparisonOperator.GREATER, lowerValue));
        expectedValueComparisons.add(new ValueComparison<Float>(ComparisonOperator.LESS_OR_EQUAL, lowerValue));
        Assert.assertEquals(crit.getValueComparisons(), expectedValueComparisons);
    }

    @Test
    public void parenthesisAroundAllTest() throws QueryNodeException {
        final String field = "color";
        final String value = "lime";
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
        final String term = "color:(lime OR amber)";
        final ICriterion criterion = parser.parse(term, DEFAULT_FIELD);

        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof OrCriterion);
    }

}
