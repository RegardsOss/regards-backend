package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import com.google.common.collect.Lists;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.AttributeCriterionBuilder;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterOperator;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;
import org.junit.Assert;
import org.junit.Test;

import java.time.OffsetDateTime;

public class AttributeCriterionBuilderTest {

    @Test
    public void buildCriterionTest() throws UnsupportedCriterionOperator {
        AttributeModel attribute = AttributeModelBuilder.build("string", PropertyType.STRING, "test string attribute")
                                                        .get();
        ICriterion crit = AttributeCriterionBuilder.build(attribute,
                                                          ParameterOperator.EQ,
                                                          Lists.newArrayList("value1"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("integer", PropertyType.INTEGER, "test integer attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.GE, Lists.newArrayList("1", "2"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("boolean", PropertyType.BOOLEAN, "test boolean attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.EQ, Lists.newArrayList("true"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("date", PropertyType.DATE_ISO8601, "test date attribute").get();
        crit = AttributeCriterionBuilder.build(attribute,
                                               ParameterOperator.EQ,
                                               Lists.newArrayList(OffsetDateTime.now().toString()));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("double", PropertyType.DOUBLE, "test double attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.LT, Lists.newArrayList("1.535"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("double", PropertyType.LONG, "test long attribute").get();
        crit = AttributeCriterionBuilder.build(attribute,
                                               ParameterOperator.GT,
                                               Lists.newArrayList("123456789123456789"));
        Assert.assertNotNull(crit);

        attribute = AttributeModelBuilder.build("url", PropertyType.URL, "test url attribute").get();
        crit = AttributeCriterionBuilder.build(attribute, ParameterOperator.EQ, Lists.newArrayList("http://plop.test"));
        Assert.assertNotNull(crit);
    }

}
