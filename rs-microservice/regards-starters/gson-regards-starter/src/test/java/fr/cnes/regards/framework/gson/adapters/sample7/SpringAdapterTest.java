/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.adapters.sample7;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;

/**
 * Test {@link GsonTypeAdapterFactoryBean}
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SpringAdapterConfiguration.class })
public class SpringAdapterTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAdapterTest.class);

    /**
     * Globally configured GSON builder
     */
    @Autowired
    private GsonBuilder builder;

    /**
     * Reference to custom factory
     */
    @Autowired
    private CustomPolymorphicTypeAdapterFactory factory;

    @Test
    public void testSample7() {
        Assert.assertNotNull(builder);
        Assert.assertNotNull(factory);

        // Create GSON from globally configured builder
        final Gson gson = builder.create();

        // Init object
        final Mission mission = new Mission();
        mission.setName("mission");
        mission.setDescription("mission description");

        List<AbstractProperty<?>> properties = new ArrayList<>();

        StringProperty str = new StringProperty();
        str.setName("string");
        str.setValue("string_val");
        properties.add(str);

        StringProperty crs = new StringProperty();
        crs.setName("CRS");
        crs.setValue("1, 2, 3");

        ObjectProperty obj = new ObjectProperty();
        obj.setName("GEO");
        List<AbstractProperty<?>> objProps = new ArrayList<>();
        objProps.add(crs);
        obj.setValue(objProps);

        properties.add(obj);

        mission.setProperties(properties);

        // Serialize with GSON
        String jsonMission = gson.toJson(mission);
        LOGGER.info(jsonMission);

        // Deserialize with GSON
        Mission animal = gson.fromJson(jsonMission, Mission.class);
        checkMission(animal, 2);

        // Add unsupported property
        BooleanProperty bp = new BooleanProperty();
        bp.setName("active");
        bp.setValue(false);
        properties.add(bp);

        // Try to serialize
        try {
            jsonMission = gson.toJson(mission);
            Assert.fail("Test should fail because attribute \"active\" is not registered.");
        } catch (JsonParseException e) {
            LOGGER.info("Serialization fails because attribute \"active\" is not registered.");
        }

        // Registering new attribute through factory bean
        factory.registerSubtype(BooleanProperty.class, "active");

        // Try to serialize
        jsonMission = gson.toJson(mission);
        LOGGER.info(jsonMission);

        // Deserialize with GSON
        animal = gson.fromJson(jsonMission, Mission.class);
        checkMission(animal, 3);
    }

    private void checkMission(Mission pMission, int pExpectedSize) {
        Assert.assertTrue(pMission instanceof Mission);

        List<AbstractProperty<?>> ppts = pMission.getProperties();
        Assert.assertTrue(ppts instanceof List);
        Assert.assertEquals(pExpectedSize, ppts.size());

        for (AbstractProperty<?> ppt : ppts) {
            if ("string".equals(ppt.getName())) {
                Assert.assertEquals("string_val", ppt.getValue());
            }
            if ("GEO".equals(ppt.getName())) {
                Assert.assertTrue(ppt instanceof ObjectProperty);
            }
            if ("active".equals(ppt.getName())) {
                Assert.assertTrue(ppt instanceof BooleanProperty);
            }
        }
    }
}
