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

import fr.cnes.regards.framework.geojson.geometry.IGeometry;

/**
 * Test GeoJson feature (de)serialization
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class FeatureTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryTest.class);

    @Autowired
    private Gson gson;

    @Test
    public void createFeatureCollection() {

        Feature feature1 = new Feature();
        feature1.setGeometry(IGeometry.point(IGeometry.position(10.0, 20.0)));

        Feature feature2 = new Feature();
        feature2.setGeometry(IGeometry.multiPoint(IGeometry.position(5.0, 5.0), IGeometry.position(25.0, 25.0)));

        FeatureCollection collection = new FeatureCollection();
        collection.add(feature1);
        collection.add(feature2);

        String collectionString = gson.toJson(collection);
        LOGGER.debug(collectionString);

        // Read
        FeatureCollection readCollection = gson.fromJson(collectionString, FeatureCollection.class);
        Assert.assertNotNull(readCollection.getFeatures());
        Assert.assertTrue(readCollection.getFeatures().size() == 2);
        Assert.assertTrue(readCollection.getFeatures().get(0) instanceof Feature);
    }
}
