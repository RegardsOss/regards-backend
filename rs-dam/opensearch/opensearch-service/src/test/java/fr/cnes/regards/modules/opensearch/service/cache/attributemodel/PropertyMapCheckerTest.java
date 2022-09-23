/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.opensearch.service.cache.attributemodel;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.gson.IAttributeHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test attribute property map algorithm
 *
 * @author Marc Sordi
 */
public class PropertyMapCheckerTest {

    private static final String TENANT = "tenant";

    private static final int STATIC_ATTRIBUTE_MODELS_NB = 12;

    private AttributeFinder finder;

    private IAttributeHelper attributeHelper;

    private List<AttributeModel> attributeModels;

    @Before
    public void init() {
        attributeModels = new ArrayList<>();

        attributeHelper = Mockito.mock(IAttributeHelper.class);

        finder = new AttributeFinder(attributeHelper,
                                     Mockito.mock(ISubscriber.class),
                                     Mockito.mock(IRuntimeTenantResolver.class));
    }

    private Map<String, AttributeModel> getBuiltMap(List<AttributeModel> attributeModels) {
        Mockito.when(attributeHelper.getAllAttributes()).thenReturn(attributeModels);

        finder.computePropertyMap(TENANT);
        // Return built map
        return finder.getPropertyMap().get(TENANT);
    }

    private void assertCount(Map<String, AttributeModel> builtMap, int expectedDynamics) {
        Assert.assertNotNull(builtMap);

        int countStatics = 0;
        int countDynamics = 0;
        for (Map.Entry<String, AttributeModel> entry : builtMap.entrySet()) {
            if (entry.getValue().isDynamic()) {
                countDynamics++;
            } else {
                countStatics++;
            }
        }

        Assert.assertEquals(STATIC_ATTRIBUTE_MODELS_NB, countStatics);
        Assert.assertEquals(expectedDynamics, countDynamics);
    }

    @Test
    public void conflict() {
        // Define attributes
        attributeModels.add(new AttributeModelBuilder(StaticProperties.FEATURE_TAGS,
                                                      PropertyType.BOOLEAN,
                                                      "Conflictual dynamic tags").build());

        // Build and get map
        Map<String, AttributeModel> builtMap = getBuiltMap(attributeModels);

        assertCount(builtMap, 2);
    }

    @Test
    public void baseProperty() {
        // Define attributes
        String startDate = "START_DATE";
        AttributeModel startDateModel = new AttributeModelBuilder(startDate,
                                                                  PropertyType.DATE_ISO8601,
                                                                  "Start date").build();
        attributeModels.add(startDateModel);

        // Build and get map
        Map<String, AttributeModel> builtMap = getBuiltMap(attributeModels);

        assertCount(builtMap, 3);

        Assert.assertTrue(builtMap.containsKey(startDate));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getJsonPathForNamespace(StaticProperties.FEATURE_PROPERTIES)));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getFullJsonPath()));
    }

    @Test
    public void fragmentProperty() {
        // Define attributes
        String startDate = "START_DATE";
        String fragment = "fragment";
        AttributeModel startDateModel = new AttributeModelBuilder(startDate,
                                                                  PropertyType.DATE_ISO8601,
                                                                  "Start date").setFragment(Fragment.buildFragment(
            fragment,
            "description")).build();
        attributeModels.add(startDateModel);

        // Build and get map
        Map<String, AttributeModel> builtMap = getBuiltMap(attributeModels);

        assertCount(builtMap, 4);

        Assert.assertTrue(builtMap.containsKey(startDate));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getJsonPathForNamespace("")));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getJsonPathForNamespace(StaticProperties.FEATURE_PROPERTIES)));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getFullJsonPath()));
    }

    @Test
    public void conflicts() {
        // Define attributes
        String startDate = "START_DATE";
        AttributeModel startDateModel = new AttributeModelBuilder(startDate,
                                                                  PropertyType.DATE_ISO8601,
                                                                  "Start date").setFragment(Fragment.buildFragment(
            "fragment1",
            "description")).build();
        attributeModels.add(startDateModel);

        // Define conflictual attribute
        AttributeModel startDateModel2 = new AttributeModelBuilder(startDate,
                                                                   PropertyType.DATE_ISO8601,
                                                                   "Start date 2").setFragment(Fragment.buildFragment(
            "fragment2",
            "description")).build();
        attributeModels.add(startDateModel2);

        // Build and get map
        Map<String, AttributeModel> builtMap = getBuiltMap(attributeModels);

        assertCount(builtMap, 6);

        Assert.assertTrue(!builtMap.containsKey(startDate));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getJsonPathForNamespace("")));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getJsonPathForNamespace(StaticProperties.FEATURE_PROPERTIES)));
        Assert.assertTrue(builtMap.containsKey(startDateModel.getFullJsonPath()));
        Assert.assertTrue(builtMap.containsKey(startDateModel2.getJsonPathForNamespace("")));
        Assert.assertTrue(builtMap.containsKey(startDateModel2.getJsonPathForNamespace(StaticProperties.FEATURE_PROPERTIES)));
        Assert.assertTrue(builtMap.containsKey(startDateModel2.getFullJsonPath()));
    }
}
