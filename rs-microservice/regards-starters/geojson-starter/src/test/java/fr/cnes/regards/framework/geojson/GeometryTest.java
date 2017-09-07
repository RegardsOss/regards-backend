/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.geojson;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.geometry.Unlocated;

/**
 * Test GeoJson serialization
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class GeometryTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryTest.class);

    private static final String GEOMETRY = "geometry";

    @Autowired
    private Gson gson;

    @Test
    public void unlocated() {
        String id = "myId";

        // Write
        Feature<String> feature = new Feature<>();
        feature.setId(id);
        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assert.assertTrue(jsonObject.get(GEOMETRY).isJsonNull());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getId().equals(id));
        Assert.assertTrue(readFeature.getGeometry() instanceof Unlocated);
    }

    @Test
    public void point() {
        Feature<String> feature = new Feature<>();
        // feature.setGeometry(geometry);
    }
}
