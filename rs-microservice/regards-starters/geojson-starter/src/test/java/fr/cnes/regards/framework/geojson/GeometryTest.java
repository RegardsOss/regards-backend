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
package fr.cnes.regards.framework.geojson;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test GeoJson geometry (de)serialization
 *
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class GeometryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryTest.class);

    private static final String GEOMETRY = "geometry";

    private static final String GEOMETRIES = "geometries";

    private static final String TYPE = "type";

    private static final String COORDINATES = "coordinates";

    @Autowired
    private Gson gson;

    @Test
    public void unlocated() {
        String id = "myId";

        // Write
        Feature feature = new Feature();
        feature.setId(id);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assert.assertTrue(jsonObject.get(GEOMETRY).isJsonNull());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertEquals(readFeature.getId(), id);
        Assert.assertTrue(readFeature.getGeometry() instanceof Unlocated);
    }

    @Test
    public void point() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createPoint());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.POINT, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertEquals(2, coordinates.getAsJsonArray().size());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof Point);
    }

    @Test
    public void multipoint() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createMultiPoint());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.MULTIPOINT, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertEquals(2, coordinates.getAsJsonArray().size());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof MultiPoint);
    }

    @Test
    public void linestring() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createLineString());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.LINESTRING, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertEquals(2, coordinates.getAsJsonArray().size());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof LineString);
    }

    @Test
    public void multilinestring() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createMultiLineString());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.MULTILINESTRING, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertEquals(2, coordinates.getAsJsonArray().size());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof MultiLineString);
    }

    @Test
    public void polygon() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createPolygon());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.POLYGON, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertEquals(2, coordinates.getAsJsonArray().size());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof Polygon);
    }

    @Test
    public void multiPolygon() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createMultiPolygon());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.MULTIPOLYGON, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertEquals(2, coordinates.getAsJsonArray().size());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof MultiPolygon);
    }

    @Test
    public void geometryCollection() {

        // Write
        Feature feature = new Feature();
        feature.setGeometry(GeometryFactory.createGeometryCollection());

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.GEOMETRY_COLLECTION, geometryObject.get(TYPE).getAsString());
        Assert.assertTrue(geometryObject.has(GEOMETRIES));

        // Get geometries
        JsonArray geometries = geometryObject.get(GEOMETRIES).getAsJsonArray();
        Assert.assertEquals(2, geometries.size());

        // Get first geometry
        geometryObject = geometries.get(0).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.POINT, geometryObject.get(TYPE).getAsString());

        // Get second geometry
        geometryObject = geometries.get(1).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertEquals(GeoJsonType.LINESTRING, geometryObject.get(TYPE).getAsString());

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof GeometryCollection);
    }
}
