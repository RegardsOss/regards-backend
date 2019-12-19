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

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.cycle.detection.invalid1.SamplePluginWithPojoCycleDetected;
import fr.cnes.regards.framework.utils.cycle.detection.invalid2.SamplePluginWithPojoCycleDetectedLevelThree;
import fr.cnes.regards.framework.utils.cycle.detection.valid.SamplePluginWithPojo;
import fr.cnes.regards.framework.utils.cycle.detection.valid.SamplePluginWithPojoWithSet;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.basic.PluginUtilsTest;
import fr.cnes.regards.framework.utils.plugins.basic.SamplePlugin;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * Unit testing of {@link PluginUtils}.
 * @author Christophe Mertz
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CycleDetectionTest {

    @Test
    public void cycleDetectionOK() throws NotAvailablePluginConfigurationException {

        PluginUtils.setup(SamplePluginWithPojo.class.getPackage().getName());

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

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE),
                     IPluginParam.build(SamplePluginWithPojo.FIELD_NAME_COEF, 12345),
                     IPluginParam.build(SamplePluginWithPojo.FIELD_NAME_POJO,
                                        PluginParameterTransformer.toJson(pojoParam)),
                     IPluginParam.build(SamplePluginWithPojo.FIELD_NAME_SUFFIX, "chris_test_1"));

        PluginConfiguration conf = PluginConfiguration.build(SamplePluginWithPojo.class, "", parameters);
        SamplePluginWithPojo samplePlugin = PluginUtils.getPlugin(conf, new HashMap<>());

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
        PluginUtils.setup(SamplePluginWithPojoCycleDetected.class.getPackage().getName());
        Assert.fail();
    }

    @Test
    public void cycleDetectedWithSet() throws NotAvailablePluginConfigurationException {

        PluginUtils.setup(SamplePluginWithPojoWithSet.class.getPackage().getName());

        TestPojoWithSet pojoParent = new TestPojoWithSet();

        TestPojoChildWithSet pojoChild = new TestPojoChildWithSet();

        TestPojo pojoParam = new TestPojo();
        pojoParam.setValue("pojo");
        pojoParam.addIntValues(1999);

        pojoParent.addChild(pojoChild);
        pojoChild.addPojo(pojoParam);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SamplePluginWithPojoWithSet.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE),
                     IPluginParam.build(SamplePluginWithPojoWithSet.FIELD_NAME_COEF, 12345),
                     IPluginParam.build(SamplePluginWithPojoWithSet.FIELD_NAME_POJO,
                                        PluginParameterTransformer.toJson(pojoParent)),
                     IPluginParam.build(SamplePluginWithPojoWithSet.FIELD_NAME_SUFFIX, "suffix"));

        // instantiate plugin

        PluginConfiguration conf = PluginConfiguration.build(SamplePluginWithPojoWithSet.class, "", parameters);
        SamplePluginWithPojoWithSet samplePlugin = PluginUtils.getPlugin(conf, new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        Assert.assertNotNull(samplePlugin.getPojo());
        Assert.assertNotNull(samplePlugin.getPojo().getChilds());
        Assert.assertEquals(samplePlugin.getPojo().getChilds().size(), pojoParent.getChilds().size());
        Assert.assertEquals(samplePlugin.getPojo().getChilds(), pojoParent.getChilds());
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cycleDetectedWithThreeLevel() throws NotAvailablePluginConfigurationException {
        PluginUtils.setup(SamplePluginWithPojoCycleDetectedLevelThree.class.getPackage().getName());
        Assert.fail();
    }
}
