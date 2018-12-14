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
package fr.cnes.regards.framework.gson.adapters.sample6;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;

/**
 * @author Marc Sordi
 */
public class AdapterTest {

    /**
     * String att key
     */
    public static final String SAMPLE_ATT = "sample";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterTest.class);

    /**
     * Geo
     */
    private static final String GEO = "GEO";

    /**
     * String att value
     */
    private static final String STRING_VAL = "string_val";

    /**
     * CRS att key
     */
    private static final String CRS = "CRS";

    /**
     * Test custom adapter factory
     */
    @Test
    public void testSample6() {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new CustomPolymorphicTypeAdapterFactory());
        gsonBuilder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe());
        final Gson gson = gsonBuilder.create();

        final Mission mission = new Mission();
        mission.setName("mission");
        mission.setDescription("mission description");

        final List<AbstractProperty<?>> properties = new ArrayList<>();

        // Root property
        final StringProperty str = new StringProperty();
        str.setName(SAMPLE_ATT);
        str.setValue(STRING_VAL);
        properties.add(str);

        // in GEO property
        final StringProperty crs = new StringProperty();
        crs.setName(CRS);
        crs.setValue("1, 2, 3");

        // in GEO property
        final DateProperty dp = new DateProperty();
        dp.setName(SAMPLE_ATT);
        dp.setValue(LocalDateTime.now());

        // GEO property
        final ObjectProperty obj = new ObjectProperty();
        obj.setName(GEO);
        final List<AbstractProperty<?>> objProps = new ArrayList<>();
        objProps.add(dp);
        objProps.add(crs);
        obj.setValue(objProps);
        properties.add(obj);

        // in CONTACT property
        final StringProperty phone = new StringProperty();
        phone.setName("phone");
        phone.setValue("0561176500");

        // CONTACT property
        final ObjectProperty contact = new ObjectProperty();
        contact.setName("CONTACT");
        final List<AbstractProperty<?>> contactProps = new ArrayList<>();
        contactProps.add(phone);
        contact.setValue(contactProps);
        properties.add(contact);

        mission.setProperties(properties);

        final String jsonMission = gson.toJson(mission);
        LOGGER.info(jsonMission);
        final Mission parsedMission = gson.fromJson(jsonMission, Mission.class);

        Assert.assertTrue(parsedMission instanceof Mission);

        final List<AbstractProperty<?>> ppts = parsedMission.getProperties();
        Assert.assertTrue(ppts instanceof List);
        final int expectedSize = 3;
        Assert.assertEquals(expectedSize, ppts.size());

        for (AbstractProperty<?> ppt : ppts) {
            if (SAMPLE_ATT.equals(ppt.getName())) {
                Assert.assertEquals(STRING_VAL, ppt.getValue());
            }
            if (GEO.equals(ppt.getName())) {
                Assert.assertTrue(ppt instanceof ObjectProperty);
                final ObjectProperty geo = (ObjectProperty) ppt;
                for (AbstractProperty<?> nestedPpt : geo.getValue()) {
                    Assert.assertTrue(SAMPLE_ATT.equals(nestedPpt.getName()) || CRS.equals(nestedPpt.getName()));
                }
            }
        }

    }
}
