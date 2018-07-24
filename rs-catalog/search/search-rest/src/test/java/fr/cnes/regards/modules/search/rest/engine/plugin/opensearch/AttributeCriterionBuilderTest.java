package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import java.time.OffsetDateTime;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;

public class AttributeCriterionBuilderTest {

    @Test
    public void buildCriterionTest() throws UnsupportedCriterionOperator {
        AttributeModel attribute = AttributeModelBuilder.build("string", AttributeType.STRING, "test string attribute")
                .get();
        ICriterion crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.EQ,
                                                          Lists.newArrayList("value1"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("integer", AttributeType.INTEGER, "test integer attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.GE, Lists.newArrayList("1", "2"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("boolean", AttributeType.BOOLEAN, "test boolean attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.EQ, Lists.newArrayList("true"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("date", AttributeType.DATE_ISO8601, "test date attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.EQ,
                                               Lists.newArrayList(OffsetDateTime.now().toString()));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("double", AttributeType.DOUBLE, "test double attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.LT, Lists.newArrayList("1.535"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("double", AttributeType.LONG, "test long attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.GT,
                                               Lists.newArrayList("123456789123456789"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("url", AttributeType.URL, "test url attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.EQ, Lists.newArrayList("http://plop.test"));
        Assert.assertNotNull(crit);
    }

}
