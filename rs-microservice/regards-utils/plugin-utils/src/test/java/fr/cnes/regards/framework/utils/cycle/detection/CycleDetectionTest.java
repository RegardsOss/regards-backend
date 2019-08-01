/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.utils.cycle.detection;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * Unit testing of {@link PluginUtils}.
 * @author Christophe Mertz
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CycleDetectionTest {

    private static final String PLUGIN_PACKAGE = "fr.cnes.regards.framework.utils.plugins";

    @Test
    public void cycleDetectionOK() throws NotAvailablePluginConfigurationException {
        List<String> values = new ArrayList<>();
        values.add("test1");
        values.add("test2");
        OffsetDateTime ofdt = OffsetDateTime.now().minusDays(5);

        TestPojo pojoParam = new TestPojo();
        pojoParam.setValue("first");
        pojoParam.setValues(values);
        pojoParam.setDate(ofdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoParam.addIntValues(1);
        pojoParam.addIntValues(2);
        pojoParam.addIntValues(3);
        pojoParam.addIntValues(4);

        /*
         * Set all parameters
         */
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePluginWithPojo.FIELD_NAME_ACTIVE, true)
                .addParameter(SamplePluginWithPojo.FIELD_NAME_COEF, 12345)
                .addParameter(SamplePluginWithPojo.FIELD_NAME_POJO, pojoParam)
                .addParameter(SamplePluginWithPojo.FIELD_NAME_SUFFIX, "chris_test_1").getParameters();

        // instantiate plugin
        PluginUtils.setup(PLUGIN_PACKAGE);
        SamplePluginWithPojo samplePlugin = PluginUtils.getPlugin(parameters, SamplePluginWithPojo.class,
                                                                  new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(samplePlugin.getPojo().getValue(), pojoParam.getValue());
        Assert.assertEquals(samplePlugin.getPojo().getValues().size(), values.size());
        Assert.assertEquals(OffsetDateTime.parse(samplePlugin.getPojo().getDate(),
                                                 DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            ofdt);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cycleDetectedWithTwoLevel() throws NotAvailablePluginConfigurationException {
        List<String> values = new ArrayList<>();
        values.add("test1");
        values.add("test2");
        values.add("test3");
        values.add("test4");
        OffsetDateTime ofdt = OffsetDateTime.now().minusYears(10);

        TestPojoParent pojoParent = new TestPojoParent();
        pojoParent.setValue("parent");
        pojoParent.setValues(values);
        pojoParent.setDate(ofdt.minusHours(55).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoParent.addIntValues(99);
        pojoParent.addIntValues(98);
        pojoParent.addIntValues(97);

        TestPojoChild pojoChild = new TestPojoChild();
        pojoChild.setValue("child");
        pojoChild.setValues(values);
        pojoChild.setDate(ofdt.minusHours(1999).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoChild.addIntValues(101);
        pojoChild.addIntValues(102);
        pojoChild.addIntValues(103);

        TestPojoParent otherPojoParent = new TestPojoParent();
        pojoParent.setValue("other parent");
        pojoParent.setValues(values);
        pojoParent.setDate(ofdt.minusSeconds(3333).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoParent.addIntValues(501);
        pojoParent.addIntValues(502);

        pojoParent.setChild(pojoChild);
        pojoChild.setParent(otherPojoParent);

        /*
         * Set all parameters
         */
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePluginWithPojoCycleDetected.FIELD_NAME_ACTIVE, true)
                .addParameter(SamplePluginWithPojoCycleDetected.FIELD_NAME_COEF, 12345)
                .addParameter(SamplePluginWithPojoCycleDetected.FIELD_NAME_POJO, pojoParent).getParameters();

        // instantiate plugin
        PluginUtils.setup(PLUGIN_PACKAGE);
        PluginUtils.getPlugin(parameters, SamplePluginWithPojoCycleDetected.class, new HashMap<>());

        Assert.fail();
    }

    @Test
    public void cycleDetectedWithSet() throws NotAvailablePluginConfigurationException {
        TestPojoWithSet pojoParent = new TestPojoWithSet();

        TestPojoChildWithSet pojoChild = new TestPojoChildWithSet();

        TestPojo pojoParam = new TestPojo();
        pojoParam.setValue("pojo");
        pojoParam.addIntValues(1999);

        pojoParent.addChild(pojoChild);
        pojoChild.addPojo(pojoParam);

        /*
         * Set all parameters
         */
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePluginWithPojo.FIELD_NAME_ACTIVE, true)
                .addParameter(SamplePluginWithPojo.FIELD_NAME_COEF, 12345)
                .addParameter(SamplePluginWithPojo.FIELD_NAME_POJO, pojoParent)
                .addParameter(SamplePluginWithPojo.FIELD_NAME_SUFFIX, "suffix").getParameters();

        // instantiate plugin
        PluginUtils.setup(PLUGIN_PACKAGE);
        SamplePluginWithPojoWithSet samplePlugin = PluginUtils.getPlugin(parameters, SamplePluginWithPojoWithSet.class,
                                                                         new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        Assert.assertNotNull(samplePlugin.getPojo());
        Assert.assertNotNull(samplePlugin.getPojo().getChilds());
        Assert.assertEquals(samplePlugin.getPojo().getChilds().size(), pojoParent.getChilds().size());
        Assert.assertEquals(samplePlugin.getPojo().getChilds(), pojoParent.getChilds());
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cycleDetectedWithThreeLevel() throws NotAvailablePluginConfigurationException {
        List<String> values = new ArrayList<>();
        values.add("test1");
        values.add("test2");
        values.add("test3");
        values.add("test4");
        OffsetDateTime ofdt = OffsetDateTime.now().minusYears(10);

        TestPojoGrandParent pojoGrandParent = new TestPojoGrandParent();
        pojoGrandParent.setValue("grand parent");
        pojoGrandParent.setValues(values);
        pojoGrandParent.setDate(ofdt.minusHours(55).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoGrandParent.addIntValues(65);

        TestPojoParent pojoParent = new TestPojoParent();
        pojoParent.setValue("parent");
        pojoParent.setValues(values);
        pojoParent.setDate(ofdt.minusHours(55).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoParent.addIntValues(99);
        pojoParent.addIntValues(98);
        pojoParent.addIntValues(97);

        TestPojoChild pojoChild = new TestPojoChild();
        pojoChild.setValue("child");
        pojoChild.setValues(values);
        pojoChild.setDate(ofdt.minusHours(1999).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoChild.addIntValues(101);
        pojoChild.addIntValues(102);
        pojoChild.addIntValues(103);

        TestPojoParent otherPojoParent = new TestPojoParent();
        pojoParent.setValue("other parent");
        pojoParent.setValues(values);
        pojoParent.setDate(ofdt.minusSeconds(3333).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pojoParent.addIntValues(501);
        pojoParent.addIntValues(502);

        pojoGrandParent.setChild(pojoParent);
        pojoParent.setChild(pojoChild);
        pojoChild.setParent(otherPojoParent);

        /*
         * Set all parameters
         */
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePluginWithPojoCycleDetectedLevelThree.FIELD_NAME_ACTIVE, true)
                .addParameter(SamplePluginWithPojoCycleDetectedLevelThree.FIELD_NAME_COEF, 12345)
                .addParameter(SamplePluginWithPojoCycleDetectedLevelThree.FIELD_NAME_POJO, pojoGrandParent)
                .getParameters();

        // instantiate plugin
        PluginUtils.setup(PLUGIN_PACKAGE);
        PluginUtils.getPlugin(parameters, SamplePluginWithPojoCycleDetectedLevelThree.class, new HashMap<>());

        Assert.fail();
    }
}
