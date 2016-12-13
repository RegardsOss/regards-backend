/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample6;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author Marc Sordi
 *
 */
public class AdapterTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterTest.class);

    /**
     * Test custom adapter factory
     */
    @Test
    public void testSample6() {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new CustomPolymorphicTypeAdapterFactory());
        final Gson gson = gsonBuilder.create();

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

        final String jsonMission = gson.toJson(mission);
        LOGGER.info(jsonMission);
        final Mission animal = gson.fromJson(jsonMission, Mission.class);

        Assert.assertTrue(animal instanceof Mission);

        List<AbstractProperty<?>> ppts = animal.getProperties();
        Assert.assertTrue(ppts instanceof List);
        Assert.assertEquals(2, ppts.size());

        for (AbstractProperty<?> ppt : ppts) {
            if ("string".equals(ppt.getName())) {
                Assert.assertEquals("string_val", ppt.getValue());
            }
            if ("GEO".equals(ppt.getName())) {
                Assert.assertTrue(ppt instanceof ObjectProperty);
            }
        }

    }
}
